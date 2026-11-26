package com.sneakerstore.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerVoucherDto {
    private Long id; // customerVoucherId if PERSONAL, null if PUBLIC
    private Long voucherId;
    private String code; // personalCode if PERSONAL, voucher.code if PUBLIC
    private String baseVoucherCode;
    private String name;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private String channel;
    private String voucherType; // PUBLIC or PRIVATE
    private String sourceType;  // PERSONAL or PUBLIC
    private String status;      // AVAILABLE, USED, EXPIRED
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private Long remainingDays;
    private String ruleName;
    private boolean eligible;
    private BigDecimal estimatedDiscount;
    private BigDecimal missingAmount;
    private String ineligibleReason;
}
