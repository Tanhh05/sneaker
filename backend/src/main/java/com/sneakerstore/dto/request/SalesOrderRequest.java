package com.sneakerstore.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SalesOrderRequest {
    private Long customerId;
    private Long employeeId;
    private String paymentMethod = "CASH";
    private String status = "COMPLETED";
    private String note;
    private String paymentReference;
    private String voucherCode;

    // Tùy chọn phát hành hóa đơn VAT ngay tại POS
    private String invoiceType; // RETAIL (mặc định) hoặc VAT
    private String vatCompanyName;
    private String vatTaxCode;
    private String vatAddress;
    private String vatEmail;
    private java.math.BigDecimal vatTaxRate;
    private Boolean autoSendInvoiceEmail;

    @NotEmpty(message = "Đơn hàng phải có ít nhất 1 sản phẩm")
    @Valid
    private List<SalesOrderItemRequest> items;

    @Data
    public static class SalesOrderItemRequest {
        @NotNull(message = "Biến thể sản phẩm không được để trống")
        private Long productVariantId;

        @NotNull(message = "Số lượng mua không được để trống")
        @Min(value = 1, message = "Số lượng mua phải từ 1 trở lên")
        private Integer quantity;
    }
}

