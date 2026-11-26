package com.sneakerstore.dto.request;

import jakarta.validation.constraints.NotNull;
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
public class InvoiceIssueRequest {

    @NotNull(message = "Vui lòng chọn đơn hàng nguồn")
    private Long orderId;

    private String invoiceType; // RETAIL or VAT

    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String customerAddress;
    private String companyName;
    private String taxCode;

    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal defaultTaxRate; // 0, 5, 8, 10
    private Boolean taxIncluded; // true = tách thuế từ đơn giá bán; false = cộng thêm VAT

    private Boolean autoSendEmail;
    private String note;

    private List<ItemTaxOverride> itemTaxOverrides;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemTaxOverride {
        private Long productVariantId;
        private BigDecimal taxRate;
        private BigDecimal discountAmount;
    }
}
