package com.sneakerstore.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherUsageDto {
    private Long id;
    private Long voucherId;
    private String voucherCode;
    private String voucherName;
    private Long orderId;
    private String orderCode;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String channel;
    private BigDecimal orderSubtotal;
    private BigDecimal discountAmount;
    private String status;
    private LocalDateTime usedAt;
    private LocalDateTime refundedAt;
    private String note;
}
