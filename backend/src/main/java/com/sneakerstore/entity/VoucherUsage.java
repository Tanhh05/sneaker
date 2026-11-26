package com.sneakerstore.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "VOUCHER_USAGES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class VoucherUsage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_voucher_usages")
    @SequenceGenerator(name = "seq_voucher_usages", sequenceName = "SEQ_VOUCHER_USAGES", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VOUCHER_ID", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUSTOMER_VOUCHER_ID")
    private CustomerVoucher customerVoucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SALES_ORDER_ID", nullable = false)
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUSTOMER_ID")
    private Customer customer;

    @Column(name = "VOUCHER_CODE", nullable = false, length = 60)
    private String voucherCode;

    @Column(name = "CHANNEL", nullable = false, length = 20)
    private String channel; // POS, ONLINE

    @Column(name = "ORDER_SUBTOTAL", nullable = false, precision = 18, scale = 2)
    private BigDecimal orderSubtotal;

    @Column(name = "DISCOUNT_AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private String status = "APPLIED"; // APPLIED, REFUNDED

    @Column(name = "USED_AT", nullable = false)
    private LocalDateTime usedAt;

    @Column(name = "REFUNDED_AT")
    private LocalDateTime refundedAt;

    @Column(name = "NOTE", length = 500)
    private String note;

    @PrePersist
    public void prePersist() {
        initAuditTimestamps();
        if (usedAt == null) usedAt = LocalDateTime.now();
        if (status == null) status = "APPLIED";
    }
}
