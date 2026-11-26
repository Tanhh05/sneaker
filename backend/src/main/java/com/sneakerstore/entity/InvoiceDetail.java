package com.sneakerstore.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "INVOICE_DETAILS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class InvoiceDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_invoice_details")
    @SequenceGenerator(name = "seq_invoice_details", sequenceName = "SEQ_INVOICE_DETAILS", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INVOICE_ID", nullable = false)
    private Invoice invoice;

    @Column(name = "PRODUCT_ID")
    private Long productId;

    @Column(name = "PRODUCT_VARIANT_ID")
    private Long productVariantId;

    @Column(name = "PRODUCT_CODE", length = 50)
    private String productCode;

    @Column(name = "PRODUCT_NAME", nullable = false, length = 255)
    private String productName;

    @Column(name = "SKU", length = 80)
    private String sku;

    @Column(name = "VARIANT_LABEL", length = 150)
    private String variantLabel;

    @Column(name = "QUANTITY", nullable = false)
    private Integer quantity;

    @Column(name = "UNIT_PRICE", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "DISCOUNT_AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "TAX_RATE", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "TAX_AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "LINE_TOTAL", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal;

    @PrePersist
    public void prePersist() {
        initAuditTimestamps();
        if (discountAmount == null) discountAmount = BigDecimal.ZERO;
        if (taxRate == null) taxRate = BigDecimal.ZERO;
        if (taxAmount == null) taxAmount = BigDecimal.ZERO;
        if (lineTotal == null) lineTotal = BigDecimal.ZERO;
    }
}
