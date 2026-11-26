package com.sneakerstore.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private Integer totalQuantity;
    private Integer usedCount;
    private Integer remainingQuantity;
    private Integer perCustomerLimit;
    private String channel;
    private String voucherType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private String effectiveStatus;
    private LocalDateTime createdAt;
}
