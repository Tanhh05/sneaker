package com.sneakerstore.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    @Size(max = 50, message = "Mã voucher tối đa 50 ký tự")
    private String code;

    @NotBlank(message = "Tên voucher không được để trống")
    @Size(max = 200, message = "Tên voucher tối đa 200 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    @NotBlank(message = "Hình thức giảm giá không được để trống")
    private String discountType; // PERCENT, FIXED

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    private BigDecimal maxDiscountAmount;

    @DecimalMin(value = "0", message = "Đơn hàng tối thiểu không được âm")
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Min(value = 1, message = "Tổng số lượng phát hành phải >= 1")
    private Integer totalQuantity; // null = không giới hạn

    @NotNull(message = "Giới hạn số lần/khách không được để trống")
    @Min(value = 1, message = "Mỗi khách được dùng tối thiểu 1 lần")
    private Integer perCustomerLimit = 1;

    @NotBlank(message = "Phạm vi kênh áp dụng không được để trống")
    private String channel = "ALL"; // ALL, POS, ONLINE

    @NotBlank(message = "Loại voucher không được để trống")
    private String voucherType = "PUBLIC"; // PUBLIC, PRIVATE

    @NotNull(message = "Ngày bắt đầu hiệu lực không được để trống")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc hiệu lực không được để trống")
    private LocalDateTime endDate;

    private String status = "ACTIVE";
}
