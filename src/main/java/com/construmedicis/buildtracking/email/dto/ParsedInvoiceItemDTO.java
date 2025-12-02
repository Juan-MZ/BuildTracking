package com.construmedicis.buildtracking.email.dto;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ParsedInvoiceItemDTO {

    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private BigDecimal taxAmount;
    private String itemCode; // Código del producto/servicio si está disponible
}
