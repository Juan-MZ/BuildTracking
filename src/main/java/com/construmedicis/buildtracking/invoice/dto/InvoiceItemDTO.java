package com.construmedicis.buildtracking.invoice.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemDTO {

    private Long id;
    private Long invoiceId;
    private Long itemId; // Puede ser null si es ítem personalizado
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private BigDecimal taxAmount;
}
