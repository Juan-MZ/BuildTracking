package com.construmedicis.buildtracking.invoice.repository;

import com.construmedicis.buildtracking.invoice.models.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByProjectId(Long projectId);

    List<Invoice> findBySupplierId(String supplierId);

    List<Invoice> findByIssueDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<Invoice> findByAssignmentConfidenceLessThan(Integer confidence);
}
