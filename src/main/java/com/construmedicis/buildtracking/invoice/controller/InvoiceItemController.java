package com.construmedicis.buildtracking.invoice.controller;

import com.construmedicis.buildtracking.invoice.dto.InvoiceItemDTO;
import com.construmedicis.buildtracking.invoice.services.InvoiceItemService;
import com.construmedicis.buildtracking.util.response.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoice-items")
public class InvoiceItemController {

    private final InvoiceItemService service;

    public InvoiceItemController(InvoiceItemService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Response<List<InvoiceItemDTO>>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<InvoiceItemDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<Response<InvoiceItemDTO>> create(@RequestBody InvoiceItemDTO invoiceItemDTO) {
        return ResponseEntity.ok(service.save(invoiceItemDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.deleteById(id));
    }

    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<Response<List<InvoiceItemDTO>>> getByInvoiceId(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(service.findByInvoiceId(invoiceId));
    }

    @GetMapping("/item/{itemId}")
    public ResponseEntity<Response<List<InvoiceItemDTO>>> getByItemId(@PathVariable Long itemId) {
        return ResponseEntity.ok(service.findByItemId(itemId));
    }
}
