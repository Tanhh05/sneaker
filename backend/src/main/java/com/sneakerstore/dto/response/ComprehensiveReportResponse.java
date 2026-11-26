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
public class ComprehensiveReportResponse {
    // Metadata bộ lọc để ghi vào Excel kế toán
    private String filterSummary;

    // Summary KPIs
    private BigDecimal totalRevenue;
    private BigDecimal totalPreTaxRevenue;
    private BigDecimal totalVatAmount;
    private BigDecimal totalCashRevenue;
    private BigDecimal totalBankTransferRevenue;
    private BigDecimal totalCodRevenue;
    private BigDecimal provisionalRevenue;
    private BigDecimal cancelledValue;
    private long totalOrders;
    private long totalCompletedOrders;
    private long totalCancelledOrders;
    private long totalQuantitySold;
    private BigDecimal aov;
    private Double deliverySuccessRate;
    private Double cancellationRate;
    private BigDecimal totalPurchaseAmount;
    private long totalConfirmedPurchases;

    // Chuyên đề 1: Doanh thu & Dòng tiền
    private List<DashboardResponse.RevenuePointDto> revenueSeries;
    private List<DashboardResponse.PaymentMethodStatDto> paymentMethodStats;

    // Chuyên đề 2: Hiệu quả Kênh bán hàng (POS vs Online)
    private List<DashboardResponse.ChannelComparisonDto> channelComparison;

    // Chuyên đề 3: Đơn hàng & Tỷ lệ Hủy/Hoàn trả
    private List<DashboardResponse.StatusCountDto> orderStatusStats;
    private List<CancelReasonStatDto> cancelReasonStats;
    private List<CancelledOrderItemDto> cancelledOrders;

    // Chuyên đề 4: Sản phẩm theo Biến thể Size, Màu sắc & Thương hiệu
    private List<DashboardResponse.ProductSalesStatDto> topSellingProducts;
    private List<DashboardResponse.ProductSalesStatDto> slowSellingProducts;
    private List<SizeSalesStatDto> sizeStats;
    private List<ColorSalesStatDto> colorStats;
    private List<BrandSalesStatDto> brandStats;

    // Chuyên đề 5: Hiệu suất Nhân viên (+ Khách hàng & Nhập hàng)
    private List<EmployeeSalesStatDto> employeeRevenueStats;
    private List<CustomerSalesStatDto> customerStats;
    private List<PurchaseSupplierStatDto> purchaseStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelReasonStatDto {
        private String reasonCategory;
        private long count;
        private BigDecimal lostValue;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelledOrderItemDto {
        private Long orderId;
        private String orderCode;
        private String channel;
        private String customerName;
        private String customerPhone;
        private LocalDateTime orderDate;
        private BigDecimal amount;
        private String paymentMethod;
        private String reasonCategory;
        private String cancelNote;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SizeSalesStatDto {
        private String sizeValue;
        private String sizeGroup; // SIZE_NU (36-39) | SIZE_NAM (40-45) | KHAC
        private long quantitySold;
        private BigDecimal revenue;
        private long currentStock;
        private Double sharePercent;
        private boolean goldenSize; // Size Vàng
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColorSalesStatDto {
        private String colorName;
        private String hexCode;
        private long quantitySold;
        private BigDecimal revenue;
        private Double sharePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrandSalesStatDto {
        private Long brandId;
        private String brandName;
        private long productCount;
        private long quantitySold;
        private BigDecimal revenue;
        private Double sharePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeSalesStatDto {
        private Long employeeId;
        private String employeeCode;
        private String employeeName;
        private long workingShifts;         // Số ca làm việc (ngày phát sinh giao dịch)
        private long completedOrderCount;
        private BigDecimal totalRevenue;
        private BigDecimal averageOrderValue; // Giá trị TB mỗi đơn tư vấn
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerSalesStatDto {
        private Long customerId;
        private String customerCode;
        private String customerName;
        private String phone;
        private long completedOrderCount;
        private BigDecimal totalSpent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseSupplierStatDto {
        private Long supplierId;
        private String supplierCode;
        private String supplierName;
        private long confirmedCount;
        private long totalItemsImported;
        private BigDecimal totalAmount;
    }
}
