package com.sneakerstore.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "VOUCHER_RULES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class VoucherRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_voucher_rules")
    @SequenceGenerator(name = "seq_voucher_rules", sequenceName = "SEQ_VOUCHER_RULES", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "RULE_NAME", nullable = false, length = 200)
    private String ruleName;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "TRIGGER_TYPE", nullable = false, length = 30)
    private String triggerType; // ORDER_COUNT, TOTAL_SPENT, SINGLE_ORDER_AMOUNT

    @Column(name = "THRESHOLD_VALUE", nullable = false, precision = 18, scale = 2)
    private BigDecimal thresholdValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VOUCHER_ID", nullable = false)
    private Voucher voucher;

    @Column(name = "MAX_AWARDS_PER_CUSTOMER", nullable = false)
    @Builder.Default
    private Integer maxAwardsPerCustomer = 1;

    @Column(name = "VALIDITY_DAYS", nullable = false)
    @Builder.Default
    private Integer validityDays = 30;

    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    @Column(name = "IS_DELETED", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @PrePersist
    public void prePersist() {
        initAuditTimestamps();
        if (maxAwardsPerCustomer == null) maxAwardsPerCustomer = 1;
        if (validityDays == null) validityDays = 30;
        if (status == null) status = "ACTIVE";
    }
}
