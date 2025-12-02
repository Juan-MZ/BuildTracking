package com.construmedicis.buildtracking.invoice.services.impl;

import com.construmedicis.buildtracking.invoice.dto.InvoiceDTO;
import com.construmedicis.buildtracking.invoice.models.Invoice;
import com.construmedicis.buildtracking.invoice.models.Invoice.PaymentStatus;
import com.construmedicis.buildtracking.invoice.repository.InvoiceRepository;
import com.construmedicis.buildtracking.invoice.services.InvoiceService;
import com.construmedicis.buildtracking.project.repository.ProjectRepository;
import com.construmedicis.buildtracking.util.exception.BusinessRuleException;
import com.construmedicis.buildtracking.util.response.Response;
import com.construmedicis.buildtracking.util.response.handler.ResponseHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ProjectRepository projectRepository;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
                             ProjectRepository projectRepository) {
        this.invoiceRepository = invoiceRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    @Transactional
    public Response<InvoiceDTO> save(InvoiceDTO invoiceDTO) {
        // Validar que el número de factura no exista
        if (invoiceDTO.getId() == null && invoiceRepository.findByInvoiceNumber(invoiceDTO.getInvoiceNumber()).isPresent()) {
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
        return new ResponseHandler<>(200, "Invoices found", "/api/invoices/project/{projectId}", invoices).getResponse();
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
        return new ResponseHandler<>(200, "Invoices found", "/api/invoices/supplier/{supplierId}", invoices).getResponse();
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
        return new ResponseHandler<>(200, "Invoices pending review", "/api/invoices/pending-review", invoices).getResponse();
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
        return new ResponseHandler<>(200, "Project assigned to invoice", "/api/invoices/{id}/assign-project", toDTO(updatedInvoice)).getResponse();
    }

    @Override
    @Transactional
    public Response<InvoiceDTO> updatePaymentStatus(Long invoiceId, PaymentStatus paymentStatus) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("invoice.not.found"));

        invoice.setPaymentStatus(paymentStatus);
        Invoice updatedInvoice = invoiceRepository.save(invoice);
        return new ResponseHandler<>(200, "Payment status updated", "/api/invoices/{id}/payment-status", toDTO(updatedInvoice)).getResponse();
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
}
