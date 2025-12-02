package com.construmedicis.buildtracking.invoice.models;

import com.construmedicis.buildtracking.item.models.Item;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_item_seq")
    @SequenceGenerator(name = "invoice_item_seq", sequenceName = "invoice_item_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonBackReference
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item; // Referencia al catálogo de ítems (puede ser null si es ítem personalizado)

    @Column(nullable = false, length = 500)
    private String description; // Descripción específica de esta línea

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice; // Precio unitario en ESTA factura

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal; // quantity * unitPrice

    @Column(precision = 19, scale = 2)
    private BigDecimal taxAmount; // IVA de esta línea
}
