package com.sneakerstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {
    private Long id;
    private String invoiceCode;
    private Long orderId;
    private String orderCode;
    private Long originalInvoiceId;
    private String originalInvoiceCode;
    private String saleChannel;
    private String invoiceType;
    private String recordKind;

    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String customerAddress;
    private String companyName;
    private String taxCode;

    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal taxableAmount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    private String paymentMethod;
    private String paymentStatus;
    private String invoiceStatus;
    private String emailStatus;

    private LocalDateTime issuedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private String cancelledBy;
    private String adjustmentReason;
    private String note;
    private String createdBy;
    private String updatedBy;

    private Long pendingCancelRequestId;
    private String pendingCancelReason;
    private String pendingCancelRequestedBy;

    private List<DetailItem> details;
    private List<HistoryItem> histories;
    private List<CancelRequestItem> cancelRequests;
    private List<EmailHistoryItem> emailHistories;
    private List<RelatedInvoiceItem> adjustmentInvoices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailItem {
        private Long id;
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
        private BigDecimal taxAmount;
        private BigDecimal lineTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItem {
        private Long id;
        private String action;
        private String fromStatus;
        private String toStatus;
        private String actor;
        private String reason;
        private String detail;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelRequestItem {
        private Long id;
        private String requestedBy;
        private String reason;
        private String status;
        private String reviewedBy;
        private String reviewNote;
        private LocalDateTime requestedAt;
        private LocalDateTime reviewedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailHistoryItem {
        private Long id;
        private String recipientEmail;
        private String subject;
        private String sendStatus;
        private String sentBy;
        private LocalDateTime sentAt;
        private Integer retryCount;
        private String errorMessage;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedInvoiceItem {
        private Long id;
        private String invoiceCode;
        private String recordKind;
        private String invoiceType;
        private String invoiceStatus;
        private BigDecimal totalAmount;
        private LocalDateTime issuedAt;
    }
}
