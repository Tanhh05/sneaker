package com.sneakerstore.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoucherRuleRequest {

    @NotBlank(message = "Tên quy tắc không được để trống")
    @Size(max = 200)
    private String ruleName;

    @Size(max = 500)
    private String description;

    @NotBlank(message = "Loại kích hoạt không được để trống")
    private String triggerType; // ORDER_COUNT, TOTAL_SPENT, SINGLE_ORDER_AMOUNT

    @NotNull(message = "Ngưỡng kích hoạt không được để trống")
    @DecimalMin(value = "1", message = "Ngưỡng kích hoạt phải >= 1")
    private BigDecimal thresholdValue;

    @NotNull(message = "Vui lòng chọn voucher phần thưởng")
    private Long voucherId;

    @NotNull
    @Min(value = 1, message = "Số lần nhận tối đa phải >= 1")
    private Integer maxAwardsPerCustomer = 1;

    @NotNull
    @Min(value = 1, message = "Số ngày hiệu lực phải >= 1")
    private Integer validityDays = 30;

    private String status = "ACTIVE";
}
