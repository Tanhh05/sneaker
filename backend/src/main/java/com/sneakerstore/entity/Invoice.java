package com.sneakerstore.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "INVOICES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_invoices")
    @SequenceGenerator(name = "seq_invoices", sequenceName = "SEQ_INVOICES", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "INVOICE_CODE", nullable = false, unique = true, length = 40)
    private String invoiceCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID", nullable = false)
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORIGINAL_INVOICE_ID")
    private Invoice originalInvoice;

    @Column(name = "SALE_CHANNEL", nullable = false, length = 20)
    private String saleChannel; // POS, ONLINE

    @Column(name = "INVOICE_TYPE", nullable = false, length = 20)
    private String invoiceType; // RETAIL, VAT

    @Column(name = "RECORD_KIND", nullable = false, length = 20)
    private String recordKind; // ORIGINAL, ADJUSTMENT, REPLACEMENT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUSTOMER_ID")
    private Customer customer;

    @Column(name = "CUSTOMER_NAME", nullable = false, length = 150)
    private String customerName;

    @Column(name = "CUSTOMER_PHONE", length = 30)
    private String customerPhone;

    @Column(name = "CUSTOMER_EMAIL", length = 150)
    private String customerEmail;

    @Column(name = "CUSTOMER_ADDRESS", length = 350)
    private String customerAddress;

    @Column(name = "COMPANY_NAME", length = 220)
    private String companyName;

    @Column(name = "TAX_CODE", length = 50)
    private String taxCode;

    @Column(name = "SUBTOTAL", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "DISCOUNT_AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "SHIPPING_FEE", nullable = false, precision = 18, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "TAXABLE_AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal taxableAmount;

    @Column(name = "TAX_RATE", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "TAX_AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "TOTAL_AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "PAYMENT_METHOD", nullable = false, length = 30)
    private String paymentMethod;

    @Column(name = "PAYMENT_STATUS", nullable = false, length = 20)
    private String paymentStatus; // PAID, UNPAID, REFUNDED

    @Column(name = "INVOICE_STATUS", nullable = false, length = 30)
    private String invoiceStatus; // ISSUED, PENDING_CANCEL, CANCELLED, ADJUSTED

    @Column(name = "EMAIL_STATUS", nullable = false, length = 20)
    private String emailStatus; // NOT_SENT, SENDING, SENT, FAILED

    @Column(name = "ISSUED_AT", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "CANCELLED_AT")
    private LocalDateTime cancelledAt;

    @Column(name = "CANCEL_REASON", length = 500)
    private String cancelReason;

    @Column(name = "CANCELLED_BY", length = 50)
    private String cancelledBy;

    @Column(name = "ADJUSTMENT_REASON", length = 500)
    private String adjustmentReason;

    @Column(name = "NOTE", length = 500)
    private String note;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceDetail> details = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        initAuditTimestamps();
        if (issuedAt == null) issuedAt = LocalDateTime.now();
        if (invoiceType == null) invoiceType = "RETAIL";
        if (recordKind == null) recordKind = "ORIGINAL";
        if (invoiceStatus == null) invoiceStatus = "ISSUED";
        if (emailStatus == null) emailStatus = "NOT_SENT";
        if (paymentStatus == null) paymentStatus = "PAID";
        if (subtotal == null) subtotal = BigDecimal.ZERO;
        if (discountAmount == null) discountAmount = BigDecimal.ZERO;
        if (shippingFee == null) shippingFee = BigDecimal.ZERO;
        if (taxableAmount == null) taxableAmount = BigDecimal.ZERO;
        if (taxRate == null) taxRate = BigDecimal.ZERO;
        if (taxAmount == null) taxAmount = BigDecimal.ZERO;
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
    }
}
