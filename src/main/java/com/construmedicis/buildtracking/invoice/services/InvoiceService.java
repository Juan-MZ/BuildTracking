package com.construmedicis.buildtracking.invoice.services;

import com.construmedicis.buildtracking.invoice.dto.InvoiceDTO;
import com.construmedicis.buildtracking.invoice.models.Invoice.PaymentStatus;
import com.construmedicis.buildtracking.util.response.Response;

import java.time.LocalDate;
import java.util.List;

public interface InvoiceService {

    Response<InvoiceDTO> save(InvoiceDTO invoiceDTO);

    Response<InvoiceDTO> findById(Long id);

    Response<List<InvoiceDTO>> findAll();

    Response<Void> deleteById(Long id);

    Response<List<InvoiceDTO>> findByProjectId(Long projectId);

    Response<List<InvoiceDTO>> findByPaymentStatus(PaymentStatus paymentStatus);

    Response<List<InvoiceDTO>> findBySupplierId(String supplierId);

    Response<List<InvoiceDTO>> findByDateRange(LocalDate startDate, LocalDate endDate);

    Response<List<InvoiceDTO>> findPendingReview(Integer maxConfidence);

    Response<InvoiceDTO> assignProject(Long invoiceId, Long projectId);

    Response<InvoiceDTO> updatePaymentStatus(Long invoiceId, PaymentStatus paymentStatus);
}
