package com.construmedicis.buildtracking.invoice.models;

import com.construmedicis.buildtracking.project.models.Project;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_seq")
    @SequenceGenerator(name = "invoice_seq", sequenceName = "invoice_sequence", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @Column(nullable = false)
    private LocalDate issueDate;

    private LocalDate dueDate;

    @Column(nullable = false)
    private String supplierId; // NIT o identificación del proveedor

    @Column(nullable = false)
    private String supplierName; // Razón social

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tax; // IVA

    @Column(precision = 19, scale = 2)
    private BigDecimal withholdingTax; // Retención en la fuente

    @Column(precision = 19, scale = 2)
    private BigDecimal withholdingICA; // Retención ICA

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceSource source;

    @Column(nullable = false)
    private Integer assignmentConfidence; // 0-100% confianza en asignación automática

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceItem> invoiceItems = new ArrayList<>();

    public enum InvoiceSource {
        MANUAL, // Ingreso manual por usuario
        EMAIL_AUTO, // Extracción automática desde email
        XML_UPLOAD // Cargue directo de archivo XML
    }

    // Método helper para agregar items
    public void addInvoiceItem(InvoiceItem item) {
        invoiceItems.add(item);
        item.setInvoice(this);
    }

    // Método helper para remover items
    public void removeInvoiceItem(InvoiceItem item) {
        invoiceItems.remove(item);
        item.setInvoice(null);
    }
}
