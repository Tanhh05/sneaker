package com.sneakerstore.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "VOUCHERS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Voucher extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_vouchers")
    @SequenceGenerator(name = "seq_vouchers", sequenceName = "SEQ_VOUCHERS", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODE", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "NAME", nullable = false, length = 200)
    private String name;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "DISCOUNT_TYPE", nullable = false, length = 20)
    private String discountType; // PERCENT, FIXED

    @Column(name = "DISCOUNT_VALUE", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "MAX_DISCOUNT_AMOUNT", precision = 18, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "MIN_ORDER_AMOUNT", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "TOTAL_QUANTITY")
    private Integer totalQuantity; // null = unlimited

    @Column(name = "USED_COUNT", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "PER_CUSTOMER_LIMIT", nullable = false)
    @Builder.Default
    private Integer perCustomerLimit = 1;

    @Column(name = "CHANNEL", nullable = false, length = 20)
    @Builder.Default
    private String channel = "ALL"; // ALL, POS, ONLINE

    @Column(name = "VOUCHER_TYPE", nullable = false, length = 20)
    @Builder.Default
    private String voucherType = "PUBLIC"; // PUBLIC, PRIVATE

    @Column(name = "START_DATE", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "END_DATE", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, EXPIRED

    @Column(name = "IS_DELETED", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @PrePersist
    public void prePersist() {
        initAuditTimestamps();
        if (minOrderAmount == null) minOrderAmount = BigDecimal.ZERO;
        if (usedCount == null) usedCount = 0;
        if (perCustomerLimit == null) perCustomerLimit = 1;
        if (channel == null) channel = "ALL";
        if (voucherType == null) voucherType = "PUBLIC";
        if (status == null) status = "ACTIVE";
    }
}
