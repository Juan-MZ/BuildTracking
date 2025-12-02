package com.construmedicis.buildtracking.invoice.dto;

import com.construmedicis.buildtracking.invoice.models.Invoice.InvoiceSource;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDTO {

    private Long id;
    private String invoiceNumber;
    private LocalDateTime issueDate;
    private LocalDateTime dueDate;
    private String supplierId;
    private String supplierName;
    private Long projectId;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal withholdingTax;
    private BigDecimal withholdingICA;
    private BigDecimal total;
    private InvoiceSource source;
    private Integer assignmentConfidence;
    private List<Long> invoiceItemIds; // IDs de las líneas de factura
}
