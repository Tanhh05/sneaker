package com.sneakerstore.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherRuleDto {
    private Long id;
    private String ruleName;
    private String description;
    private String triggerType;
    private BigDecimal thresholdValue;
    private Long voucherId;
    private String voucherCode;
    private String voucherName;
    private String voucherDiscountSummary;
    private Integer maxAwardsPerCustomer;
    private Integer validityDays;
    private String status;
    private LocalDateTime createdAt;
}
