package com.sneakerstore.repository;

import com.sneakerstore.dto.response.ComprehensiveReportResponse;
import com.sneakerstore.dto.response.DashboardResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ReportQueryRepository {

    List<DashboardResponse.RevenuePointDto> queryRevenueSeries(
            String groupBy, LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId);

    Map<String, Object> queryOrderAggregateSummary(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId);

    BigDecimal queryActualRevenue(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String paymentMethod,
            Long brandId, Long categoryId);

    List<DashboardResponse.PaymentMethodStatDto> queryPaymentMethodStats(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId);

    List<DashboardResponse.ChannelComparisonDto> queryChannelComparison(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId);

    List<DashboardResponse.StatusCountDto> queryOrderStatusCounts(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId);

    List<DashboardResponse.ProductSalesStatDto> queryProductSalesStats(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId, boolean topSelling, int limit);

    default List<DashboardResponse.ProductSalesStatDto> queryProductSalesStats(
            LocalDateTime fromDate, LocalDateTime toDate,
            Long brandId, Long categoryId, boolean topSelling, int limit) {
        return queryProductSalesStats(fromDate, toDate, null, null, null, brandId, categoryId, topSelling, limit);
    }

    List<DashboardResponse.LowStockAlertDto> queryLowStockAlerts(
            int threshold, Long brandId, Long categoryId, int limit);

    List<ComprehensiveReportResponse.CancelledOrderItemDto> queryCancelledOrders(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String paymentMethod,
            Long brandId, Long categoryId);

    List<ComprehensiveReportResponse.SizeSalesStatDto> querySizeStats(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId);

    List<ComprehensiveReportResponse.ColorSalesStatDto> queryColorStats(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId);

    List<ComprehensiveReportResponse.BrandSalesStatDto> queryBrandStats(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId);

    List<ComprehensiveReportResponse.EmployeeSalesStatDto> queryEmployeeStats(
            LocalDateTime fromDate, LocalDateTime toDate,
            String paymentMethod, Long brandId, Long categoryId);

    List<ComprehensiveReportResponse.CustomerSalesStatDto> queryCustomerStats(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String paymentMethod);

    List<ComprehensiveReportResponse.PurchaseSupplierStatDto> queryPurchaseStats(
            LocalDateTime fromDate, LocalDateTime toDate);
}
