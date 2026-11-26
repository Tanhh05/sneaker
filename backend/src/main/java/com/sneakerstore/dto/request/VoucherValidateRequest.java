package com.sneakerstore.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoucherValidateRequest {
    private String voucherCode;
    private String channel; // POS or ONLINE
    private BigDecimal orderAmount;
    private Long customerId;
    private String customerPhone;
    private String customerEmail;
}
