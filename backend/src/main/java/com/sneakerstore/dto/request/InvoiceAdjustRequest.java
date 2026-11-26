package com.sneakerstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceAdjustRequest {

    private String recordKind; // ADJUSTMENT or REPLACEMENT
    private String invoiceType; // RETAIL or VAT

    @NotBlank(message = "Vui lòng nhập lý do điều chỉnh / thay thế hóa đơn")
    private String reason;

    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String customerAddress;
    private String companyName;
    private String taxCode;

    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private Boolean taxIncluded;
    private String note;

    private List<AdjustItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdjustItem {
        private Long productId;
        private Long productVariantId;
        private String productCode;
        private String productName;
        private String sku;
        private String variantLabel;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal discountAmount;
        private BigDecimal taxRate;
    }
}
