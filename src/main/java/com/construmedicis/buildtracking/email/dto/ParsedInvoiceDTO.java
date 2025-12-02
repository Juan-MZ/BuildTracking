package com.construmedicis.buildtracking.email.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ParsedInvoiceDTO {

    private String invoiceNumber;
    private LocalDateTime issueDate;
    private LocalDateTime dueDate;
    private String supplierId; // NIT
    private String supplierName;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal withholdingTax;
    private BigDecimal withholdingICA;
    private BigDecimal total;
    private List<ParsedInvoiceItemDTO> items;
    private String rawXmlContent; // Para debugging/auditoría
}
