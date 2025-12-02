package com.construmedicis.buildtracking.invoice.repository;

import com.construmedicis.buildtracking.invoice.models.Invoice;
import com.construmedicis.buildtracking.invoice.models.Invoice.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByProjectId(Long projectId);

    List<Invoice> findByPaymentStatus(PaymentStatus paymentStatus);

    List<Invoice> findBySupplierId(String supplierId);

    List<Invoice> findByIssueDateBetween(LocalDate startDate, LocalDate endDate);

    List<Invoice> findByProjectIdAndPaymentStatus(Long projectId, PaymentStatus paymentStatus);

    List<Invoice> findByAssignmentConfidenceLessThan(Integer confidence);
}
