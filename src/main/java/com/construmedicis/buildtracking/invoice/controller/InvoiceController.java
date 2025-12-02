package com.construmedicis.buildtracking.invoice.controller;

import com.construmedicis.buildtracking.invoice.dto.InvoiceDTO;
import com.construmedicis.buildtracking.invoice.models.Invoice.PaymentStatus;
import com.construmedicis.buildtracking.invoice.services.InvoiceService;
import com.construmedicis.buildtracking.util.response.Response;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Response<List<InvoiceDTO>>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<InvoiceDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<Response<InvoiceDTO>> create(@RequestBody InvoiceDTO invoiceDTO) {
        return ResponseEntity.ok(service.save(invoiceDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.deleteById(id));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<Response<List<InvoiceDTO>>> getByProjectId(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.findByProjectId(projectId));
    }

    @GetMapping("/status/{paymentStatus}")
    public ResponseEntity<Response<List<InvoiceDTO>>> getByPaymentStatus(@PathVariable PaymentStatus paymentStatus) {
        return ResponseEntity.ok(service.findByPaymentStatus(paymentStatus));
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<Response<List<InvoiceDTO>>> getBySupplierId(@PathVariable String supplierId) {
        return ResponseEntity.ok(service.findBySupplierId(supplierId));
    }

    @GetMapping("/date-range")
    public ResponseEntity<Response<List<InvoiceDTO>>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.findByDateRange(startDate, endDate));
    }

    @GetMapping("/pending-review")
    public ResponseEntity<Response<List<InvoiceDTO>>> getPendingReview(
            @RequestParam(defaultValue = "70") Integer maxConfidence) {
        return ResponseEntity.ok(service.findPendingReview(maxConfidence));
    }

    @PutMapping("/{id}/assign-project")
    public ResponseEntity<Response<InvoiceDTO>> assignProject(
            @PathVariable Long id,
            @RequestParam Long projectId) {
        return ResponseEntity.ok(service.assignProject(id, projectId));
    }

    @PutMapping("/{id}/payment-status")
    public ResponseEntity<Response<InvoiceDTO>> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam PaymentStatus paymentStatus) {
        return ResponseEntity.ok(service.updatePaymentStatus(id, paymentStatus));
    }
}
