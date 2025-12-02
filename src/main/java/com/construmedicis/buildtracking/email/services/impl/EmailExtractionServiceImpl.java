package com.construmedicis.buildtracking.email.services.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.construmedicis.buildtracking.assignment.dto.ProjectAssignmentResultDTO;
import com.construmedicis.buildtracking.assignment.services.ProjectAssignmentRuleService;
import com.construmedicis.buildtracking.email.dto.EmailSyncResultDTO;
import com.construmedicis.buildtracking.email.dto.ParsedInvoiceDTO;
import com.construmedicis.buildtracking.email.dto.ParsedInvoiceItemDTO;
import com.construmedicis.buildtracking.email.models.EmailConfig;
import com.construmedicis.buildtracking.email.services.EmailExtractionService;
import com.construmedicis.buildtracking.email.services.GmailAuthService;
import com.construmedicis.buildtracking.email.services.InvoiceXmlParser;
import com.construmedicis.buildtracking.invoice.dto.InvoiceDTO;
import com.construmedicis.buildtracking.invoice.dto.InvoiceItemDTO;
import com.construmedicis.buildtracking.invoice.models.Invoice.InvoiceSource;
import com.construmedicis.buildtracking.invoice.models.Invoice.PaymentStatus;
import com.construmedicis.buildtracking.invoice.services.InvoiceItemService;
import com.construmedicis.buildtracking.invoice.services.InvoiceService;
import com.construmedicis.buildtracking.item.models.Item;
import com.construmedicis.buildtracking.item.services.ItemMatchingService;
import com.construmedicis.buildtracking.util.response.Response;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailExtractionServiceImpl implements EmailExtractionService {

    private final GmailAuthService gmailAuthService;
    private final InvoiceXmlParser xmlParser;
    private final InvoiceService invoiceService;
    private final InvoiceItemService invoiceItemService;
    private final ProjectAssignmentRuleService assignmentRuleService;
    private final ItemMatchingService itemMatchingService;

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/buildtracking_invoices/";

    @Override
    @Transactional
    public EmailSyncResultDTO syncEmailsForConfig(EmailConfig emailConfig) {
        EmailSyncResultDTO result = EmailSyncResultDTO.builder()
                .emailsProcessed(0)
                .invoicesCreated(0)
                .invoicesAutoAssigned(0)
                .invoicesPendingReview(0)
                .errors(new ArrayList<>())
                .build();

        try {
            // Crear directorio temporal si no existe
            Files.createDirectories(Path.of(TEMP_DIR));

            // Obtener cliente de Gmail autenticado
            Gmail gmailService = gmailAuthService.getGmailService(
                    emailConfig.getCredentialsPath(),
                    emailConfig.getTokensDirectory());

            // Buscar mensajes con la etiqueta especificada
            List<Message> messages = fetchMessages(gmailService, emailConfig);
            
            log.info("Encontrados {} mensajes para procesar", messages.size());

            // Procesar cada mensaje
            for (Message message : messages) {
                try {
                    processMessage(gmailService, message, emailConfig, result);
                    result.setEmailsProcessed(result.getEmailsProcessed() + 1);
                } catch (Exception e) {
                    log.error("Error procesando mensaje {}: {}", message.getId(), e.getMessage(), e);
                    result.getErrors().add("Error en mensaje " + message.getId() + ": " + e.getMessage());
                }
            }

            // Determinar estado final
            if (result.getErrors().isEmpty()) {
                result.setSyncStatus("SUCCESS");
            } else if (result.getInvoicesCreated() > 0) {
                result.setSyncStatus("PARTIAL_SUCCESS");
            } else {
                result.setSyncStatus("FAILED");
            }

        } catch (IOException | GeneralSecurityException e) {
            log.error("Error en sincronización de emails: {}", e.getMessage(), e);
            result.getErrors().add("Error de autenticación: " + e.getMessage());
            result.setSyncStatus("FAILED");
        }

        return result;
    }

    private List<Message> fetchMessages(Gmail gmailService, EmailConfig emailConfig) throws IOException {
        List<Message> allMessages = new ArrayList<>();

        // Construir query: buscar en la etiqueta y desde la última sincronización
        StringBuilder query = new StringBuilder("label:" + emailConfig.getGmailLabel());
        query.append(" has:attachment");
        query.append(" filename:xml");

        if (emailConfig.getLastSyncDate() != null) {
            long timestamp = emailConfig.getLastSyncDate()
                    .atZone(ZoneId.systemDefault())
                    .toEpochSecond();
            query.append(" after:").append(timestamp);
        }

        ListMessagesResponse response = gmailService.users().messages()
                .list("me")
                .setQ(query.toString())
                .setMaxResults(100L)
                .execute();

        if (response.getMessages() != null) {
            for (com.google.api.services.gmail.model.Message msg : response.getMessages()) {
                // Obtener el mensaje completo con payload
                Message fullMessage = gmailService.users().messages()
                        .get("me", msg.getId())
                        .setFormat("full")
                        .execute();
                allMessages.add(fullMessage);
            }
        }

        return allMessages;
    }

    private void processMessage(Gmail gmailService, Message message, EmailConfig emailConfig,
            EmailSyncResultDTO result) throws IOException {
        
        MessagePart payload = message.getPayload();
        if (payload == null || payload.getParts() == null) {
            log.warn("Mensaje {} no tiene adjuntos", message.getId());
            return;
        }

        // Buscar adjuntos XML
        for (MessagePart part : payload.getParts()) {
            String filename = part.getFilename();
            if (filename != null && filename.toLowerCase().endsWith(".xml")) {
                try {
                    processAttachment(gmailService, message.getId(), part, emailConfig, result);
                } catch (Exception e) {
                    log.error("Error procesando adjunto {}: {}", filename, e.getMessage(), e);
                    result.getErrors().add("Error en adjunto " + filename + ": " + e.getMessage());
                }
            }
        }
    }

    private void processAttachment(Gmail gmailService, String messageId, MessagePart part,
            EmailConfig emailConfig, EmailSyncResultDTO result) throws IOException {
        
        String filename = part.getFilename();
        String attachmentId = part.getBody().getAttachmentId();

        if (attachmentId == null) {
            log.warn("Adjunto {} no tiene attachmentId", filename);
            return;
        }

        // Descargar adjunto
        MessagePartBody attachmentData = gmailService.users().messages().attachments()
                .get("me", messageId, attachmentId)
                .execute();

        byte[] data = Base64.getUrlDecoder().decode(attachmentData.getData());

        // Guardar temporalmente
        File tempFile = new File(TEMP_DIR + filename);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(data);
        }

        log.info("Adjunto {} descargado exitosamente", filename);

        try {
            // Validar que sea un XML válido de factura
            if (!xmlParser.isValidInvoiceXml(tempFile)) {
                log.warn("Archivo {} no es una factura XML válida", filename);
                result.getErrors().add("Archivo " + filename + " no es una factura XML válida");
                return;
            }

            // Parsear XML
            ParsedInvoiceDTO parsedInvoice = xmlParser.parseXml(tempFile);

            // Crear invoice en BD
            InvoiceDTO invoiceDTO = createInvoiceFromParsed(parsedInvoice, emailConfig);
            Response<InvoiceDTO> invoiceResponse = invoiceService.save(invoiceDTO);

            if (invoiceResponse.getStatus() != 200 && invoiceResponse.getStatus() != 201) {
                result.getErrors().add("Error creando factura " + parsedInvoice.getInvoiceNumber() + 
                        ": " + invoiceResponse.getUserMessage());
                return;
            }

            InvoiceDTO savedInvoice = invoiceResponse.getData();
            result.setInvoicesCreated(result.getInvoicesCreated() + 1);

            // Crear invoice items con vinculación al catálogo de items
            for (ParsedInvoiceItemDTO parsedItem : parsedInvoice.getItems()) {
                // Buscar o crear item en el catálogo
                Item catalogItem = itemMatchingService.findOrCreateItem(parsedItem, emailConfig.getProject());
                
                // Crear invoice item vinculado al catálogo
                InvoiceItemDTO itemDTO = createInvoiceItemFromParsed(parsedItem, savedInvoice.getId());
                itemDTO.setItemId(catalogItem.getId());
                invoiceItemService.save(itemDTO);
                
                log.debug("Invoice item creado y vinculado al catálogo. Item ID: {}, Description: {}", 
                        catalogItem.getId(), parsedItem.getDescription());
            }

            // Evaluar reglas de asignación
            Response<ProjectAssignmentResultDTO> assignmentResponse = 
                    assignmentRuleService.evaluateRulesForInvoice(savedInvoice);

            if (assignmentResponse.getStatus() == 200 && assignmentResponse.getData() != null) {
                ProjectAssignmentResultDTO assignmentResult = assignmentResponse.getData();
                
                if (assignmentResult.getConfidence() >= 70) {
                    // Auto-asignar con alta confianza
                    invoiceService.assignProject(savedInvoice.getId(), assignmentResult.getProjectId());
                    result.setInvoicesAutoAssigned(result.getInvoicesAutoAssigned() + 1);
                    log.info("Factura {} auto-asignada al proyecto {} con confianza {}%",
                            savedInvoice.getInvoiceNumber(), assignmentResult.getProjectId(),
                            assignmentResult.getConfidence());
                } else {
                    // Marcar para revisión manual
                    result.setInvoicesPendingReview(result.getInvoicesPendingReview() + 1);
                    log.info("Factura {} pendiente de revisión manual (confianza {}%)",
                            savedInvoice.getInvoiceNumber(), assignmentResult.getConfidence());
                }
            } else {
                result.setInvoicesPendingReview(result.getInvoicesPendingReview() + 1);
                log.info("Factura {} sin regla de asignación, pendiente de revisión manual",
                        savedInvoice.getInvoiceNumber());
            }

        } finally {
            // IMPORTANTE: Eliminar archivo temporal
            if (tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (deleted) {
                    log.info("Archivo temporal {} eliminado", filename);
                } else {
                    log.warn("No se pudo eliminar archivo temporal {}", filename);
                }
            }
        }
    }

    private InvoiceDTO createInvoiceFromParsed(ParsedInvoiceDTO parsed, EmailConfig emailConfig) {
        return InvoiceDTO.builder()
                .invoiceNumber(parsed.getInvoiceNumber())
                .issueDate(parsed.getIssueDate())
                .dueDate(parsed.getDueDate())
                .supplierId(parsed.getSupplierId())
                .supplierName(parsed.getSupplierName())
                .subtotal(parsed.getSubtotal())
                .tax(parsed.getTax())
                .withholdingTax(parsed.getWithholdingTax())
                .withholdingICA(parsed.getWithholdingICA())
                .total(parsed.getTotal())
                .paymentStatus(PaymentStatus.PENDING)
                .source(InvoiceSource.EMAIL_AUTO)
                .assignmentConfidence(0) // Se actualizará después de evaluar reglas
                .build();
    }

    private InvoiceItemDTO createInvoiceItemFromParsed(ParsedInvoiceItemDTO parsed, Long invoiceId) {
        return InvoiceItemDTO.builder()
                .invoiceId(invoiceId)
                .description(parsed.getDescription())
                .quantity(parsed.getQuantity())
                .unitPrice(parsed.getUnitPrice())
                .lineTotal(parsed.getLineTotal())
                .taxAmount(parsed.getTaxAmount())
                .build();
    }
}
