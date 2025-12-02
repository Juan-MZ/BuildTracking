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
import com.construmedicis.buildtracking.invoice.models.InvoiceItem;
import com.construmedicis.buildtracking.invoice.repository.InvoiceRepository;
import com.construmedicis.buildtracking.invoice.services.InvoiceItemService;
import com.construmedicis.buildtracking.invoice.services.InvoiceService;
import com.construmedicis.buildtracking.item.models.Item;
import com.construmedicis.buildtracking.item.services.ItemMatchingService;
import com.construmedicis.buildtracking.item.services.ItemService;
import com.construmedicis.buildtracking.project.models.Project;
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
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
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
    private final ItemService itemService;

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/buildtracking_invoices/";

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
            ProjectRepository projectRepository,
            GmailAuthService gmailAuthService,
            InvoiceXmlParser xmlParser,
            InvoiceItemService invoiceItemService,
            ItemMatchingService itemMatchingService,
            ProjectAssignmentRuleService assignmentRuleService,
            ItemService itemService) {
        this.invoiceRepository = invoiceRepository;
        this.projectRepository = projectRepository;
        this.gmailAuthService = gmailAuthService;
        this.xmlParser = xmlParser;
        this.invoiceItemService = invoiceItemService;
        this.itemMatchingService = itemMatchingService;
        this.assignmentRuleService = assignmentRuleService;
        this.itemService = itemService;
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
    public Response<List<InvoiceDTO>> findBySupplierId(String supplierId) {
        List<InvoiceDTO> invoices = invoiceRepository.findBySupplierId(supplierId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new ResponseHandler<>(200, "Invoices found", "/api/invoices/supplier/{supplierId}", invoices)
                .getResponse();
    }

    @Override
    public Response<List<InvoiceDTO>> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
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

        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessRuleException("project.not.found"));

        invoice.setProject(project);
        invoice.setAssignmentConfidence(100); // Asignación manual tiene 100% confianza

        Invoice updatedInvoice = invoiceRepository.save(invoice);

        // Asociar los items de la factura al proyecto
        associateInvoiceItemsToProject(invoice, project);

        // Actualizar stock de cada item de la factura
        updateStockForInvoiceItems(invoice);

        return new ResponseHandler<>(200, "Project assigned to invoice", "/api/invoices/{id}/assign-project",
                toDTO(updatedInvoice)).getResponse();
    }

    @Override
    @Transactional
    public Response<InvoiceDTO> unassignProject(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("invoice.not.found"));

        if (invoice.getProject() == null) {
            throw new BusinessRuleException("invoice.not.assigned");
        }

        // Desasignar proyecto
        invoice.setProject(null);
        invoice.setAssignmentConfidence(0);

        Invoice updatedInvoice = invoiceRepository.save(invoice);

        // Actualizar stock de cada item (se reducirá porque ya no está asignado)
        updateStockForInvoiceItems(invoice);

        return new ResponseHandler<>(200, "Project unassigned from invoice", "/api/invoices/{id}/unassign-project",
                toDTO(updatedInvoice)).getResponse();
    }

    /**
     * Actualiza el stock de todos los items de una factura.
     * El stock se recalcula sumando solo las cantidades de facturas asignadas a
     * proyectos.
     */
    private void updateStockForInvoiceItems(Invoice invoice) {
        if (invoice.getInvoiceItems() == null || invoice.getInvoiceItems().isEmpty()) {
            return;
        }

        for (InvoiceItem invoiceItem : invoice.getInvoiceItems()) {
            if (invoiceItem.getItem() != null) {
                itemService.updateItemStock(invoiceItem.getItem().getId());
            }
        }
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
                .withholdingTax(invoice.getWithholdingTax() != null ? invoice.getWithholdingTax() : BigDecimal.ZERO)
                .withholdingICA(invoice.getWithholdingICA() != null ? invoice.getWithholdingICA() : BigDecimal.ZERO)
                .total(invoice.getTotal())
                .source(invoice.getSource())
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
                .source(dto.getSource())
                .assignmentConfidence(dto.getAssignmentConfidence())
                .build();

        if (dto.getProjectId() != null) {
            invoice.setProject(projectRepository.findById(dto.getProjectId()).orElse(null));
        }

        return invoice;
    }

    @Override
    @Transactional
    public Response<EmailSyncResultDTO> syncFromGmail(String gmailLabel, String after, String before) {
        EmailSyncResultDTO result = EmailSyncResultDTO.builder()
                .emailsProcessed(0)
                .invoicesCreated(0)
                .invoicesUpdated(0)
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

            // Buscar mensajes con la etiqueta y rango de fechas
            List<Message> messages = fetchMessagesFromGmail(gmailService, gmailLabel, after, before);
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
            } else if (result.getInvoicesCreated() > 0 || result.getInvoicesUpdated() > 0) {
                result.setSyncStatus("PARTIAL_SUCCESS");
            } else {
                result.setSyncStatus("FAILED");
            }

            log.info(
                    "Sincronización completada: {} creadas, {} actualizadas, {} auto-asignadas, {} pendientes revisión",
                    result.getInvoicesCreated(), result.getInvoicesUpdated(),
                    result.getInvoicesAutoAssigned(), result.getInvoicesPendingReview());

        } catch (Exception e) {
            log.error("Error en sincronización desde Gmail: {}", e.getMessage(), e);
            result.getErrors().add("Error general: " + e.getMessage());
            result.setSyncStatus("FAILED");
        }

        return new ResponseHandler<>(200, "Sincronización completada", "/api/invoices/sync-gmail", result)
                .getResponse();
    }

    private List<Message> fetchMessagesFromGmail(Gmail gmailService, String gmailLabel, String after, String before)
            throws Exception {
        List<Message> allMessages = new ArrayList<>();

        // Construir query con formato correcto para etiquetas
        String labelQuery = gmailLabel.contains(" ") || gmailLabel.contains("/")
                ? "label:\"" + gmailLabel + "\""
                : "label:" + gmailLabel;

        // Agregar filtros de fecha si están presentes (formato: yyyy/MM/dd)
        StringBuilder queryBuilder = new StringBuilder(labelQuery);
        queryBuilder.append(" has:attachment");

        if (after != null && !after.trim().isEmpty()) {
            queryBuilder.append(" after:").append(after);
        }
        if (before != null && !before.trim().isEmpty()) {
            queryBuilder.append(" before:").append(before);
        }

        String query = queryBuilder.toString();
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
        log.debug("Procesando mensaje ID: {}", message.getId());

        if (message.getPayload() == null) {
            log.warn("Mensaje {} sin payload", message.getId());
            return;
        }

        // Buscar adjuntos recursivamente (pueden estar anidados)
        List<MessagePart> attachments = findAttachments(message.getPayload());

        if (attachments.isEmpty()) {
            log.debug("Mensaje {} no tiene adjuntos", message.getId());
            return;
        }

        log.info("Mensaje {} tiene {} adjuntos", message.getId(), attachments.size());

        for (MessagePart part : attachments) {
            String filename = part.getFilename();
            if (filename != null && filename.toLowerCase().endsWith(".xml")) {
                log.info("Procesando adjunto XML directo: {}", filename);
                processXmlAttachment(gmailService, message.getId(), part, result);
            } else if (filename != null && filename.toLowerCase().endsWith(".zip")) {
                log.info("Procesando archivo ZIP: {}", filename);
                processZipAttachment(gmailService, message.getId(), part, result);
            } else if (filename != null) {
                log.debug("Adjunto omitido (no XML ni ZIP): {}", filename);
            }
        }
    }

    /**
     * Procesa un archivo ZIP adjunto, extrayendo y procesando XMLs de facturas
     * contenidos.
     */
    private void processZipAttachment(Gmail gmailService, String messageId, MessagePart part, EmailSyncResultDTO result)
            throws Exception {
        String filename = part.getFilename();
        log.info("Descargando archivo ZIP: {} del mensaje {}", filename, messageId);

        // Descargar ZIP
        String attId = part.getBody().getAttachmentId();
        if (attId == null) {
            log.warn("Adjunto ZIP {} no tiene attachmentId", filename);
            return;
        }

        MessagePartBody attachPart = gmailService.users().messages().attachments()
                .get("me", messageId, attId)
                .execute();

        byte[] zipBytes = Base64.getUrlDecoder().decode(attachPart.getData());
        File zipFile = new File(TEMP_DIR + filename);

        try (FileOutputStream fos = new FileOutputStream(zipFile)) {
            fos.write(zipBytes);
        }

        log.info("Archivo ZIP descargado: {} ({} bytes)", zipFile.getAbsolutePath(), zipBytes.length);

        // Crear directorio para extraer
        Path extractedDir = Path.of(TEMP_DIR + "extracted_" + System.currentTimeMillis());
        Files.createDirectories(extractedDir);

        try {
            // Extraer ZIP
            unzipFile(zipFile.toPath(), extractedDir);
            log.info("ZIP extraído en: {}", extractedDir);

            // Buscar XMLs en el contenido extraído
            File[] extractedFiles = extractedDir.toFile().listFiles();
            if (extractedFiles != null) {
                for (File extractedFile : extractedFiles) {
                    if (extractedFile.getName().toLowerCase().endsWith(".xml")) {
                        log.info("Procesando XML extraído de ZIP: {}", extractedFile.getName());
                        processExtractedXml(extractedFile, result);
                    } else {
                        log.debug("Archivo extraído omitido (no XML): {}", extractedFile.getName());
                    }
                }
            }
        } finally {
            // Limpiar archivos temporales
            deleteDirectory(extractedDir.toFile());
            zipFile.delete();
            log.debug("Archivos temporales eliminados: ZIP y directorio extraído");
        }
    }

    /**
     * Extrae un archivo ZIP en el directorio especificado.
     */
    private void unzipFile(Path zipPath, Path extractedDir) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                File outFile = extractedDir.resolve(entry.getName()).toFile();

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                    continue;
                }

                // Crear carpetas intermedias si es necesario
                outFile.getParentFile().mkdirs();

                try (InputStream in = zipFile.getInputStream(entry);
                        OutputStream out = new FileOutputStream(outFile)) {
                    in.transferTo(out);
                }
                log.debug("Extraído: {}", outFile.getName());
            }
        }
    }

    /**
     * Procesa un archivo XML ya extraído de un ZIP.
     */
    private void processExtractedXml(File xmlFile, EmailSyncResultDTO result) throws Exception {
        log.info("Validando XML extraído: {} ({} bytes)", xmlFile.getName(), xmlFile.length());

        try {
            // Validar XML
            if (!xmlParser.isValidInvoiceXml(xmlFile)) {
                log.warn("XML no válido (no es formato DIAN): {}", xmlFile.getName());
                return;
            }

            log.info("XML válido, parseando factura...");

            // Parsear factura
            ParsedInvoiceDTO parsedInvoice = xmlParser.parseXml(xmlFile);
            log.info("Factura parseada: {}", parsedInvoice.getInvoiceNumber());

            // Verificar si ya existe (actualizar si es corrección)
            Optional<Invoice> existingInvoice = invoiceRepository
                    .findByInvoiceNumber(parsedInvoice.getInvoiceNumber());

            Invoice savedInvoice;
            boolean isUpdate = false;

            if (existingInvoice.isPresent()) {
                // Factura existe - verificar si hay cambios
                Invoice invoice = existingInvoice.get();

                if (hasInvoiceChanged(invoice, parsedInvoice)) {
                    // Hay cambios - actualizar con nueva información (corrección)
                    log.info("Factura {} ya existe (ID: {}), actualizando con corrección",
                            parsedInvoice.getInvoiceNumber(), invoice.getId());

                    updateInvoiceFromParsed(invoice, parsedInvoice);

                    // Eliminar items antiguos
                    invoiceItemService.deleteByInvoiceId(invoice.getId());

                    savedInvoice = invoiceRepository.save(invoice);
                    isUpdate = true;
                    result.setInvoicesUpdated(result.getInvoicesUpdated() + 1);

                    log.info("Factura {} actualizada exitosamente", parsedInvoice.getInvoiceNumber());
                } else {
                    // Sin cambios - omitir actualización
                    log.info("Factura {} ya existe sin cambios, omitiendo", parsedInvoice.getInvoiceNumber());
                    return; // Salir sin procesar
                }
            } else {
                // Crear nueva factura
                InvoiceDTO invoiceDTO = createInvoiceFromParsed(parsedInvoice, xmlFile.getAbsolutePath());
                savedInvoice = fromDTO(invoiceDTO);
                savedInvoice = invoiceRepository.save(savedInvoice);
                result.setInvoicesCreated(result.getInvoicesCreated() + 1);

                log.info("Factura {} creada exitosamente", parsedInvoice.getInvoiceNumber());
            }

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

                // Actualizar confianza de asignación
                savedInvoice.setAssignmentConfidence(assignmentResult.getConfidence());

                if (assignmentResult.getConfidence() >= 70) {
                    // Auto-asignar
                    savedInvoice.setProject(projectRepository.findById(assignmentResult.getProjectId())
                            .orElseThrow(() -> new BusinessRuleException("project.not.found")));
                    savedInvoice = invoiceRepository.save(savedInvoice);
                    result.setInvoicesAutoAssigned(result.getInvoicesAutoAssigned() + 1);

                    // Asociar items al proyecto
                    associateItemsToProject(parsedInvoice.getItems(), assignmentResult.getProjectId());

                    log.info("Factura {} auto-asignada a proyecto {} (confianza: {}%)",
                            parsedInvoice.getInvoiceNumber(),
                            assignmentResult.getProjectId(),
                            assignmentResult.getConfidence());
                } else {
                    // Guardar solo la confianza sin asignar proyecto
                    invoiceRepository.save(savedInvoice);
                    result.setInvoicesPendingReview(result.getInvoicesPendingReview() + 1);
                    log.info("Factura {} pendiente revisión manual (confianza: {}%)",
                            parsedInvoice.getInvoiceNumber(), assignmentResult.getConfidence());
                }
            }
        } catch (Exception e) {
            log.error("Error procesando XML {}: {}", xmlFile.getName(), e.getMessage());
            result.getErrors().add("Error en " + xmlFile.getName() + ": " + e.getMessage());
        } finally {
            // Eliminar archivo XML temporal
            xmlFile.delete();
        }
    }

    /**
     * Elimina un directorio y todo su contenido recursivamente.
     */
    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }

    /**
     * Busca adjuntos recursivamente en todas las partes del mensaje.
     * Gmail puede anidar adjuntos en multipart/mixed, multipart/alternative, etc.
     */
    private List<MessagePart> findAttachments(MessagePart part) {
        List<MessagePart> attachments = new ArrayList<>();

        // Si esta parte tiene attachmentId, es un adjunto
        if (part.getBody() != null && part.getBody().getAttachmentId() != null) {
            attachments.add(part);
        }

        // Buscar recursivamente en las subpartes
        if (part.getParts() != null) {
            for (MessagePart subPart : part.getParts()) {
                attachments.addAll(findAttachments(subPart));
            }
        }

        return attachments;
    }

    private void processXmlAttachment(Gmail gmailService, String messageId, MessagePart part, EmailSyncResultDTO result)
            throws Exception {
        String filename = part.getFilename();
        log.info("Descargando adjunto XML: {} del mensaje {}", filename, messageId);

        // Descargar adjunto
        String attId = part.getBody().getAttachmentId();
        if (attId == null) {
            log.warn("Adjunto {} no tiene attachmentId", filename);
            return;
        }

        MessagePartBody attachPart = gmailService.users().messages().attachments()
                .get("me", messageId, attId)
                .execute();

        byte[] fileBytes = Base64.getUrlDecoder().decode(attachPart.getData());
        File tempFile = new File(TEMP_DIR + filename);

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(fileBytes);
        }

        log.info("Archivo XML descargado: {} ({} bytes)", tempFile.getAbsolutePath(), fileBytes.length);

        try {
            // Validar XML
            if (!xmlParser.isValidInvoiceXml(tempFile)) {
                log.warn("XML no válido (no es formato DIAN): {}", tempFile.getName());
                return;
            }

            log.info("XML válido, parseando factura...");

            // Parsear factura
            ParsedInvoiceDTO parsedInvoice = xmlParser.parseXml(tempFile);
            log.info("Factura parseada: {}", parsedInvoice.getInvoiceNumber());

            // Verificar si ya existe (actualizar si es corrección)
            Optional<Invoice> existingInvoice = invoiceRepository
                    .findByInvoiceNumber(parsedInvoice.getInvoiceNumber());

            Invoice savedInvoice;
            boolean isUpdate = false;

            if (existingInvoice.isPresent()) {
                // Factura existe - verificar si hay cambios
                Invoice invoice = existingInvoice.get();

                if (hasInvoiceChanged(invoice, parsedInvoice)) {
                    // Hay cambios - actualizar con nueva información (corrección)
                    log.info("Factura {} ya existe (ID: {}), actualizando con corrección",
                            parsedInvoice.getInvoiceNumber(), invoice.getId());

                    updateInvoiceFromParsed(invoice, parsedInvoice);

                    // Eliminar items antiguos
                    invoiceItemService.deleteByInvoiceId(invoice.getId());

                    savedInvoice = invoiceRepository.save(invoice);
                    isUpdate = true;
                    result.setInvoicesUpdated(result.getInvoicesUpdated() + 1);

                    log.info("Factura {} actualizada exitosamente", parsedInvoice.getInvoiceNumber());
                } else {
                    // Sin cambios - omitir actualización
                    log.info("Factura {} ya existe sin cambios, omitiendo", parsedInvoice.getInvoiceNumber());
                    return; // Salir sin procesar
                }
            } else {
                // Crear nueva factura
                InvoiceDTO invoiceDTO = createInvoiceFromParsed(parsedInvoice, tempFile.getAbsolutePath());
                savedInvoice = fromDTO(invoiceDTO);
                savedInvoice = invoiceRepository.save(savedInvoice);
                result.setInvoicesCreated(result.getInvoicesCreated() + 1);

                log.info("Factura {} creada exitosamente", parsedInvoice.getInvoiceNumber());
            }

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

    /**
     * Asocia los items de una factura existente a un proyecto.
     * Se usa cuando se asigna manualmente una factura a un proyecto.
     */
    /**
     * Actualiza el stock de todos los items de una factura.
     * El stock se recalcula sumando solo las cantidades de facturas asignadas a
     * proyectos.
     */
    private void associateInvoiceItemsToProject(Invoice invoice, Project project) {
        if (invoice.getInvoiceItems() == null || invoice.getInvoiceItems().isEmpty()) {
            log.warn("Factura {} no tiene items para asociar al proyecto {}",
                    invoice.getId(), project.getId());
            return;
        }

        for (InvoiceItem invoiceItem : invoice.getInvoiceItems()) {
            if (invoiceItem.getItem() != null) {
                Item catalogItem = invoiceItem.getItem();

                // Asociar el item al proyecto si aún no está asociado
                if (!catalogItem.getProjects().contains(project)) {
                    catalogItem.getProjects().add(project);
                    log.info("Item {} asociado manualmente a proyecto {}",
                            catalogItem.getId(), project.getId());
                }
            }
        }
    }

    /**
     * Verifica si los datos parseados son diferentes a la factura existente.
     * Retorna true si hay cambios que ameriten actualizar.
     */
    private boolean hasInvoiceChanged(Invoice existing, ParsedInvoiceDTO parsed) {
        // Normalizar valores null a BigDecimal.ZERO para comparación
        BigDecimal existingWithholdingTax = existing.getWithholdingTax() != null
                ? existing.getWithholdingTax()
                : BigDecimal.ZERO;
        BigDecimal parsedWithholdingTax = parsed.getWithholdingTax() != null
                ? parsed.getWithholdingTax()
                : BigDecimal.ZERO;
        BigDecimal existingWithholdingICA = existing.getWithholdingICA() != null
                ? existing.getWithholdingICA()
                : BigDecimal.ZERO;
        BigDecimal parsedWithholdingICA = parsed.getWithholdingICA() != null
                ? parsed.getWithholdingICA()
                : BigDecimal.ZERO;

        // Comparar campos principales
        return !existing.getIssueDate().equals(parsed.getIssueDate())
                || !java.util.Objects.equals(existing.getDueDate(), parsed.getDueDate()) // dueDate puede ser null
                || !existing.getSupplierId().equals(parsed.getSupplierId())
                || !existing.getSupplierName().equals(parsed.getSupplierName())
                || existing.getSubtotal().compareTo(parsed.getSubtotal()) != 0
                || existing.getTax().compareTo(parsed.getTax()) != 0
                || existingWithholdingTax.compareTo(parsedWithholdingTax) != 0
                || existingWithholdingICA.compareTo(parsedWithholdingICA) != 0
                || existing.getTotal().compareTo(parsed.getTotal()) != 0;
    }

    /**
     * Actualiza una factura existente con datos parseados (para correcciones).
     */
    private void updateInvoiceFromParsed(Invoice invoice, ParsedInvoiceDTO parsed) {
        invoice.setIssueDate(parsed.getIssueDate());
        invoice.setDueDate(parsed.getDueDate());
        invoice.setSupplierId(parsed.getSupplierId());
        invoice.setSupplierName(parsed.getSupplierName());
        invoice.setSubtotal(parsed.getSubtotal());
        invoice.setTax(parsed.getTax());
        invoice.setWithholdingTax(parsed.getWithholdingTax() != null ? parsed.getWithholdingTax() : BigDecimal.ZERO);
        invoice.setWithholdingICA(parsed.getWithholdingICA() != null ? parsed.getWithholdingICA() : BigDecimal.ZERO);
        invoice.setTotal(parsed.getTotal());
        // Mantener project y assignmentConfidence de la factura original
        // Resetear confianza a 0 para re-evaluar reglas con nueva información
        invoice.setAssignmentConfidence(0);
        log.info("Factura {} actualizada: subtotal={}, tax={}, total={}",
                invoice.getInvoiceNumber(), invoice.getSubtotal(), invoice.getTax(), invoice.getTotal());
    }

    private InvoiceDTO createInvoiceFromParsed(ParsedInvoiceDTO parsed) {
        return createInvoiceFromParsed(parsed, null);
    }

    private InvoiceDTO createInvoiceFromParsed(ParsedInvoiceDTO parsed, String xmlFilePath) {
        return InvoiceDTO.builder()
                .invoiceNumber(parsed.getInvoiceNumber())
                .issueDate(parsed.getIssueDate())
                .dueDate(parsed.getDueDate())
                .supplierId(parsed.getSupplierId())
                .supplierName(parsed.getSupplierName())
                .subtotal(parsed.getSubtotal())
                .tax(parsed.getTax())
                .withholdingTax(parsed.getWithholdingTax() != null ? parsed.getWithholdingTax() : BigDecimal.ZERO)
                .withholdingICA(parsed.getWithholdingICA() != null ? parsed.getWithholdingICA() : BigDecimal.ZERO)
                .total(parsed.getTotal())
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
