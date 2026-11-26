package com.sneakerstore.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherValidationResponse {
    private boolean valid;
    private String message;
    private Long voucherId;
    private Long customerVoucherId;
    private String appliedCode;
    private String voucherName;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private BigDecimal orderAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private BigDecimal missingAmount;
}
