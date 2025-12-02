package com.construmedicis.buildtracking.invoice.services.impl;

import com.construmedicis.buildtracking.assignment.dto.ProjectAssignmentResultDTO;
import com.construmedicis.buildtracking.assignment.services.ProjectAssignmentRuleService;
import com.construmedicis.buildtracking.email.dto.EmailSyncResultDTO;
import com.construmedicis.buildtracking.email.dto.ParsedInvoiceDTO;
import com.construmedicis.buildtracking.email.dto.ParsedInvoiceItemDTO;
import com.construmedicis.buildtracking.email.services.GmailAuthService;
import com.construmedicis.buildtracking.email.services.InvoiceXmlParser;
import com.construmedicis.buildtracking.invoice.dto.InvoiceDTO;
import com.construmedicis.buildtracking.invoice.dto.InvoiceItemDTO;
import com.construmedicis.buildtracking.invoice.models.Invoice;
import com.construmedicis.buildtracking.invoice.models.Invoice.InvoiceSource;
import com.construmedicis.buildtracking.invoice.models.Invoice.PaymentStatus;
import com.construmedicis.buildtracking.invoice.repository.InvoiceRepository;
import com.construmedicis.buildtracking.invoice.services.InvoiceItemService;
import com.construmedicis.buildtracking.invoice.services.InvoiceService;
import com.construmedicis.buildtracking.item.models.Item;
import com.construmedicis.buildtracking.item.services.ItemMatchingService;
import com.construmedicis.buildtracking.project.repository.ProjectRepository;
import com.construmedicis.buildtracking.util.exception.BusinessRuleException;
import com.construmedicis.buildtracking.util.response.Response;
import com.construmedicis.buildtracking.util.response.handler.ResponseHandler;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ProjectRepository projectRepository;
    private final GmailAuthService gmailAuthService;
    private final InvoiceXmlParser xmlParser;
    private final InvoiceItemService invoiceItemService;
    private final ItemMatchingService itemMatchingService;
    private final ProjectAssignmentRuleService assignmentRuleService;

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/buildtracking_invoices/";

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
            ProjectRepository projectRepository,
            GmailAuthService gmailAuthService,
            InvoiceXmlParser xmlParser,
            InvoiceItemService invoiceItemService,
            ItemMatchingService itemMatchingService,
            ProjectAssignmentRuleService assignmentRuleService) {
        this.invoiceRepository = invoiceRepository;
        this.projectRepository = projectRepository;
        this.gmailAuthService = gmailAuthService;
        this.xmlParser = xmlParser;
        this.invoiceItemService = invoiceItemService;
        this.itemMatchingService = itemMatchingService;
        this.assignmentRuleService = assignmentRuleService;
    }

    @Override
    @Transactional
    public Response<InvoiceDTO> save(InvoiceDTO invoiceDTO) {
        // Validar que el número de factura no exista
        if (invoiceDTO.getId() == null
                && invoiceRepository.findByInvoiceNumber(invoiceDTO.getInvoiceNumber()).isPresent()) {
            throw new BusinessRuleException("invoice.duplicate.number");
        }

        // Validar que el proyecto exista si se proporciona
        if (invoiceDTO.getProjectId() != null && !projectRepository.existsById(invoiceDTO.getProjectId())) {
            throw new BusinessRuleException("project.not.found");
        }

        // Calcular totales si no vienen calculados
        if (invoiceDTO.getTotal() == null) {
            calculateTotals(invoiceDTO);
        }

        Invoice invoice = fromDTO(invoiceDTO);
        Invoice savedInvoice = invoiceRepository.save(invoice);
        return new ResponseHandler<>(201, "Invoice created", "/api/invoices", toDTO(savedInvoice)).getResponse();
    }

    @Override
    public Response<InvoiceDTO> findById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("invoice.not.found"));
        return new ResponseHandler<>(200, "Invoice found", "/api/invoices/{id}", toDTO(invoice)).getResponse();
    }

    @Override
    public Response<List<InvoiceDTO>> findAll() {
        List<InvoiceDTO> invoices = invoiceRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new ResponseHandler<>(200, "Invoices found", "/api/invoices", invoices).getResponse();
    }

    @Override
    @Transactional
    public Response<Void> deleteById(Long id) {
        if (!invoiceRepository.existsById(id)) {
            throw new BusinessRuleException("invoice.not.found");
        }
        invoiceRepository.deleteById(id);
        return new ResponseHandler<Void>(200, "Invoice deleted", "/api/invoices/{id}", null).getResponse();
    }

    @Override
    public Response<List<InvoiceDTO>> findByProjectId(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new BusinessRuleException("project.not.found");
        }
        List<InvoiceDTO> invoices = invoiceRepository.findByProjectId(projectId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new ResponseHandler<>(200, "Invoices found", "/api/invoices/project/{projectId}", invoices)
                .getResponse();
    }

    @Override
    public Response<List<InvoiceDTO>> findByPaymentStatus(PaymentStatus paymentStatus) {
        List<InvoiceDTO> invoices = invoiceRepository.findByPaymentStatus(paymentStatus).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new ResponseHandler<>(200, "Invoices found", "/api/invoices", invoices).getResponse();
    }

    @Override
    public Response<List<InvoiceDTO>> findBySupplierId(String supplierId) {
        List<InvoiceDTO> invoices = invoiceRepository.findBySupplierId(supplierId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new ResponseHandler<>(200, "Invoices found", "/api/invoices/supplier/{supplierId}", invoices)
                .getResponse();
    }

    @Override
    public Response<List<InvoiceDTO>> findByDateRange(LocalDate startDate, LocalDate endDate) {
        List<InvoiceDTO> invoices = invoiceRepository.findByIssueDateBetween(startDate, endDate).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new ResponseHandler<>(200, "Invoices found", "/api/invoices", invoices).getResponse();
    }

    @Override
    public Response<List<InvoiceDTO>> findPendingReview(Integer maxConfidence) {
        List<InvoiceDTO> invoices = invoiceRepository.findByAssignmentConfidenceLessThan(maxConfidence).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new ResponseHandler<>(200, "Invoices pending review", "/api/invoices/pending-review", invoices)
                .getResponse();
    }

    @Override
    @Transactional
    public Response<InvoiceDTO> assignProject(Long invoiceId, Long projectId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("invoice.not.found"));

        if (!projectRepository.existsById(projectId)) {
            throw new BusinessRuleException("project.not.found");
        }

        invoice.setProject(projectRepository.findById(projectId).get());
        invoice.setAssignmentConfidence(100); // Asignación manual tiene 100% confianza

        Invoice updatedInvoice = invoiceRepository.save(invoice);
        return new ResponseHandler<>(200, "Project assigned to invoice", "/api/invoices/{id}/assign-project",
                toDTO(updatedInvoice)).getResponse();
    }

    @Override
    @Transactional
    public Response<InvoiceDTO> updatePaymentStatus(Long invoiceId, PaymentStatus paymentStatus) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("invoice.not.found"));

        invoice.setPaymentStatus(paymentStatus);
        Invoice updatedInvoice = invoiceRepository.save(invoice);
        return new ResponseHandler<>(200, "Payment status updated", "/api/invoices/{id}/payment-status",
                toDTO(updatedInvoice)).getResponse();
    }

    private void calculateTotals(InvoiceDTO invoiceDTO) {
        // Calcular total: subtotal + tax - retenciones
        BigDecimal total = invoiceDTO.getSubtotal()
                .add(invoiceDTO.getTax() != null ? invoiceDTO.getTax() : BigDecimal.ZERO)
                .subtract(invoiceDTO.getWithholdingTax() != null ? invoiceDTO.getWithholdingTax() : BigDecimal.ZERO)
                .subtract(invoiceDTO.getWithholdingICA() != null ? invoiceDTO.getWithholdingICA() : BigDecimal.ZERO);

        invoiceDTO.setTotal(total);
    }

    private InvoiceDTO toDTO(Invoice invoice) {
        return InvoiceDTO.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .supplierId(invoice.getSupplierId())
                .supplierName(invoice.getSupplierName())
                .projectId(invoice.getProject() != null ? invoice.getProject().getId() : null)
                .subtotal(invoice.getSubtotal())
                .tax(invoice.getTax())
                .withholdingTax(invoice.getWithholdingTax())
                .withholdingICA(invoice.getWithholdingICA())
                .total(invoice.getTotal())
                .paymentStatus(invoice.getPaymentStatus())
                .source(invoice.getSource())
                .xmlFilePath(invoice.getXmlFilePath())
                .assignmentConfidence(invoice.getAssignmentConfidence())
                .invoiceItemIds(invoice.getInvoiceItems().stream()
                        .map(item -> item.getId())
                        .collect(Collectors.toList()))
                .build();
    }

    private Invoice fromDTO(InvoiceDTO dto) {
        Invoice invoice = Invoice.builder()
                .id(dto.getId())
                .invoiceNumber(dto.getInvoiceNumber())
                .issueDate(dto.getIssueDate())
                .dueDate(dto.getDueDate())
                .supplierId(dto.getSupplierId())
                .supplierName(dto.getSupplierName())
                .subtotal(dto.getSubtotal())
                .tax(dto.getTax())
                .withholdingTax(dto.getWithholdingTax())
                .withholdingICA(dto.getWithholdingICA())
                .total(dto.getTotal())
                .paymentStatus(dto.getPaymentStatus())
                .source(dto.getSource())
                .xmlFilePath(dto.getXmlFilePath())
                .assignmentConfidence(dto.getAssignmentConfidence())
                .build();

        if (dto.getProjectId() != null) {
            invoice.setProject(projectRepository.findById(dto.getProjectId()).orElse(null));
        }

        return invoice;
    }

    @Override
    @Transactional
    public Response<EmailSyncResultDTO> syncFromGmail(String gmailLabel) {
        EmailSyncResultDTO result = EmailSyncResultDTO.builder()
                .emailsProcessed(0)
                .invoicesCreated(0)
                .invoicesAutoAssigned(0)
                .invoicesPendingReview(0)
                .errors(new ArrayList<>())
                .syncStatus("IN_PROGRESS")
                .build();

        try {
            // Crear directorio temporal
            Files.createDirectories(Path.of(TEMP_DIR));

            // Obtener Gmail service autenticado
            Gmail gmailService = gmailAuthService.getGmailService(
                    "src/main/resources/credentials.json",
                    "src/main/resources/tokens");

            // Buscar mensajes con la etiqueta
            List<Message> messages = fetchMessagesFromGmail(gmailService, gmailLabel);
            result.setEmailsProcessed(messages.size());

            log.info("Encontrados {} correos con etiqueta '{}'", messages.size(), gmailLabel);

            // Procesar cada mensaje
            for (Message message : messages) {
                try {
                    processMessageForInvoice(gmailService, message, result);
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

            log.info("Sincronización completada: {} facturas creadas, {} auto-asignadas, {} pendientes revisión",
                    result.getInvoicesCreated(), result.getInvoicesAutoAssigned(), result.getInvoicesPendingReview());

        } catch (Exception e) {
            log.error("Error en sincronización desde Gmail: {}", e.getMessage(), e);
            result.getErrors().add("Error general: " + e.getMessage());
            result.setSyncStatus("FAILED");
        }

        return new ResponseHandler<>(200, "Sincronización completada", "/api/invoices/sync-gmail", result)
                .getResponse();
    }

    private List<Message> fetchMessagesFromGmail(Gmail gmailService, String gmailLabel) throws Exception {
        List<Message> allMessages = new ArrayList<>();

        // Construir query con formato correcto para etiquetas
        String labelQuery = gmailLabel.contains(" ") || gmailLabel.contains("/")
                ? "label:\"" + gmailLabel + "\""
                : "label:" + gmailLabel;

        String query = labelQuery + " has:attachment";
        log.info("Gmail query: {}", query);

        ListMessagesResponse response = gmailService.users().messages()
                .list("me")
                .setQ(query)
                .setMaxResults(100L)
                .execute();

        if (response.getMessages() != null) {
            log.info("Encontrados {} mensajes", response.getMessages().size());
            for (com.google.api.services.gmail.model.Message msg : response.getMessages()) {
                Message fullMessage = gmailService.users().messages()
                        .get("me", msg.getId())
                        .setFormat("full")
                        .execute();
                allMessages.add(fullMessage);
            }
        } else {
            log.warn("No se encontraron mensajes con query: {}", query);
        }

        return allMessages;
    }

    private void processMessageForInvoice(Gmail gmailService, Message message, EmailSyncResultDTO result)
            throws Exception {
        if (message.getPayload() == null || message.getPayload().getParts() == null) {
            return;
        }

        for (MessagePart part : message.getPayload().getParts()) {
            String filename = part.getFilename();
            if (filename != null && filename.toLowerCase().endsWith(".xml")) {
                processXmlAttachment(gmailService, message.getId(), part, result);
            }
        }
    }

    private void processXmlAttachment(Gmail gmailService, String messageId, MessagePart part, EmailSyncResultDTO result)
            throws Exception {
        // Descargar adjunto
        String attId = part.getBody().getAttachmentId();
        MessagePartBody attachPart = gmailService.users().messages().attachments()
                .get("me", messageId, attId)
                .execute();

        byte[] fileBytes = Base64.getUrlDecoder().decode(attachPart.getData());
        File tempFile = new File(TEMP_DIR + part.getFilename());

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(fileBytes);
        }

        try {
            // Validar XML
            if (!xmlParser.isValidInvoiceXml(tempFile)) {
                log.warn("XML no válido: {}", tempFile.getName());
                return;
            }

            // Parsear factura
            ParsedInvoiceDTO parsedInvoice = xmlParser.parseXml(tempFile);

            // Verificar si ya existe (evitar duplicados)
            Optional<Invoice> existingInvoice = invoiceRepository.findByInvoiceNumber(parsedInvoice.getInvoiceNumber());
            if (existingInvoice.isPresent()) {
                log.info("Factura {} ya existe, omitiendo", parsedInvoice.getInvoiceNumber());
                return;
            }

            // Crear factura
            InvoiceDTO invoiceDTO = createInvoiceFromParsed(parsedInvoice);
            Invoice savedInvoice = fromDTO(invoiceDTO);
            savedInvoice = invoiceRepository.save(savedInvoice);
            result.setInvoicesCreated(result.getInvoicesCreated() + 1);

            log.info("Factura {} creada exitosamente", parsedInvoice.getInvoiceNumber());

            // Crear items y vincular al catálogo
            for (ParsedInvoiceItemDTO parsedItem : parsedInvoice.getItems()) {
                // Buscar o crear item en catálogo (sin proyecto por ahora)
                Item catalogItem = itemMatchingService.findOrCreateItem(parsedItem, null);

                // Crear invoice item vinculado
                InvoiceItemDTO itemDTO = createInvoiceItemFromParsed(parsedItem, savedInvoice.getId());
                itemDTO.setItemId(catalogItem.getId());
                invoiceItemService.save(itemDTO);
            }

            // Evaluar reglas de asignación automática
            Response<ProjectAssignmentResultDTO> assignmentResponse = assignmentRuleService
                    .evaluateRulesForInvoice(toDTO(savedInvoice));

            if (assignmentResponse.getStatus() == 200 && assignmentResponse.getData() != null) {
                ProjectAssignmentResultDTO assignmentResult = assignmentResponse.getData();

                if (assignmentResult.getConfidence() >= 70) {
                    // Auto-asignar
                    assignProject(savedInvoice.getId(), assignmentResult.getProjectId());
                    result.setInvoicesAutoAssigned(result.getInvoicesAutoAssigned() + 1);

                    // Ahora asociar items al proyecto
                    associateItemsToProject(parsedInvoice.getItems(), assignmentResult.getProjectId());

                    log.info("Factura {} auto-asignada a proyecto {} (confianza {}%)",
                            parsedInvoice.getInvoiceNumber(), assignmentResult.getProjectId(),
                            assignmentResult.getConfidence());
                } else {
                    result.setInvoicesPendingReview(result.getInvoicesPendingReview() + 1);
                    log.info("Factura {} pendiente revisión manual (confianza {}%)",
                            parsedInvoice.getInvoiceNumber(), assignmentResult.getConfidence());
                }
            } else {
                result.setInvoicesPendingReview(result.getInvoicesPendingReview() + 1);
            }

        } finally {
            // Eliminar archivo temporal
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private void associateItemsToProject(List<ParsedInvoiceItemDTO> items, Long projectId) {
        var project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return;
        }

        for (ParsedInvoiceItemDTO parsedItem : items) {
            Item item = itemMatchingService.findOrCreateItem(parsedItem, project);
            log.debug("Item {} asociado a proyecto {}", item.getId(), projectId);
        }
    }

    private InvoiceDTO createInvoiceFromParsed(ParsedInvoiceDTO parsed) {
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
