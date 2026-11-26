package com.sneakerstore.dto.response;

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
public class DashboardResponse {
    // Legacy operational counters (backward compatibility)
    private long totalProducts;
    private long activeProducts;
    private long totalCustomers;
    private long totalEmployees;
    private long totalOrders;
    private long pendingOrders;
    private BigDecimal todayRevenue;
    private BigDecimal monthRevenue;
    private BigDecimal totalRevenue;

    // Executive Overview KPIs (Section III.1)
    private BigDecimal actualRevenue;          // Doanh thu thực nhận (COMPLETED)
    private BigDecimal previousActualRevenue;  // Doanh thu thực nhận kỳ trước
    private Double revenueGrowthPercent;       // % tăng/giảm so với kỳ trước
    private BigDecimal provisionalRevenue;     // Doanh thu tạm tính (PENDING/CONFIRMED/SHIPPING/DRAFT)
    private long provisionalOrderCount;
    private BigDecimal cancelledValue;         // Giá trị đơn bị hủy / bom hàng
    private long cancelledOrderCount;
    private long completedOrderCount;
    private BigDecimal aov;                    // Giá trị trung bình 1 đơn hàng
    private Double deliverySuccessRate;        // Tỷ lệ giao hàng thành công (%)
    private Double cancellationRate;           // Tỷ lệ hủy đơn (%)

    // Visualizations & Rankings (Section III.2 & III.3)
    private List<RevenuePointDto> revenueTrend;
    private List<PaymentMethodStatDto> paymentMethodStats;
    private List<ChannelComparisonDto> channelComparison;
    private List<FunnelStageDto> orderFunnel;
    private List<ProductSalesStatDto> topSellingProducts;
    private List<LowStockAlertDto> lowStockAlerts;

    // Legacy series (backward compatibility)
    private List<RevenuePointDto> revenueByDay;
    private List<RevenuePointDto> revenueByMonth;
    private List<RevenuePointDto> revenueByYear;
    private List<StatusCountDto> orderStatusCounts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSalesStatDto {
        private Long productId;
        private String productCode;
        private String productName;
        private String thumbnailUrl;
        private String brandName;
        private String categoryName;
        private long quantitySold;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenuePointDto {
        private String period;
        private long orderCount;
        private long quantitySold;
        private BigDecimal preTaxRevenue;
        private BigDecimal vatAmount;
        private BigDecimal cashRevenue;
        private BigDecimal bankTransferRevenue;
        private BigDecimal codRevenue;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusCountDto {
        private String status;
        private long count;
        private BigDecimal totalAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodStatDto {
        private String paymentMethod; // CASH, BANK_TRANSFER, COD
        private String label;
        private long orderCount;
        private BigDecimal amount;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelComparisonDto {
        private String channel; // POS, ONLINE
        private String label;
        private long totalOrders;
        private long completedOrders;
        private long cancelledOrders;
        private long processingOrders;
        private BigDecimal actualRevenue;
        private BigDecimal cancelledValue;
        private BigDecimal shippingFee;
        private BigDecimal aov;
        private Double successRate;
        private Double cancellationRate;
        private Double instantPaymentRate;
        private Double revenueSharePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunnelStageDto {
        private String stageKey;
        private String label;
        private long count;
        private BigDecimal amount;
        private Double rateFromTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LowStockAlertDto {
        private Long variantId;
        private Long productId;
        private String productCode;
        private String productName;
        private String thumbnailUrl;
        private String brandName;
        private String sku;
        private String sizeValue;
        private String colorName;
        private String hexCode;
        private int availableQuantity;
    }
}
