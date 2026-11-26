package com.sneakerstore.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CUSTOMER_VOUCHERS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CustomerVoucher extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_customer_vouchers")
    @SequenceGenerator(name = "seq_customer_vouchers", sequenceName = "SEQ_CUSTOMER_VOUCHERS", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUSTOMER_ID", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VOUCHER_ID", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RULE_ID")
    private VoucherRule rule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SOURCE_ORDER_ID")
    private SalesOrder sourceOrder;

    @Column(name = "PERSONAL_CODE", nullable = false, unique = true, length = 60)
    private String personalCode;

    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private String status = "AVAILABLE"; // AVAILABLE, USED, EXPIRED, REVOKED

    @Column(name = "ISSUED_AT", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "USED_AT")
    private LocalDateTime usedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USED_ORDER_ID")
    private SalesOrder usedOrder;

    @Column(name = "AWARD_EMAIL_SENT", nullable = false)
    @Builder.Default
    private boolean awardEmailSent = false;

    @Column(name = "EXPIRY_REMINDED_3D_DATE")
    private LocalDate expiryReminded3dDate;

    @Column(name = "EXPIRY_REMINDED_1D_DATE")
    private LocalDate expiryReminded1dDate;

    @PrePersist
    public void prePersist() {
        initAuditTimestamps();
        if (issuedAt == null) issuedAt = LocalDateTime.now();
        if (status == null) status = "AVAILABLE";
    }
}
