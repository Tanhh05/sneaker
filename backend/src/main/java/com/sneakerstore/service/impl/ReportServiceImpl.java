package com.sneakerstore.service.impl;

import com.sneakerstore.common.constant.AppConstants;
import com.sneakerstore.dto.response.ComprehensiveReportResponse;
import com.sneakerstore.dto.response.DashboardResponse;
import com.sneakerstore.exporter.ReportExcelExporter;
import com.sneakerstore.repository.*;
import com.sneakerstore.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final ReportQueryRepository reportQueryRepository;
    private final ReportExcelExporter reportExcelExporter;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData(
            String groupBy, LocalDate fromDate, LocalDate toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId) {

        LocalDateTime start = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime end = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
        String cleanGroup = (groupBy != null && !groupBy.isBlank()) ? groupBy.toUpperCase() : "DAY";

        // Operational counters
        long totalProducts = productRepository.count();
        long activeProducts = productRepository.countByStatus(AppConstants.STATUS_ACTIVE);
        long totalCustomers = customerRepository.count();
        long totalEmployees = employeeRepository.count();

        LocalDate today = LocalDate.now();
        BigDecimal todayRevenue = nvl(salesOrderRepository.sumCompletedRevenue(today.atStartOfDay(), today.atTime(LocalTime.MAX)));
        BigDecimal monthRevenue = nvl(salesOrderRepository.sumCompletedRevenue(today.withDayOfMonth(1).atStartOfDay(), today.atTime(LocalTime.MAX)));
        BigDecimal allTimeRevenue = nvl(salesOrderRepository.sumCompletedRevenue(null, null));

        // Multidimensional summary for active filter window
        Map<String, Object> agg = reportQueryRepository.queryOrderAggregateSummary(
                start, end, channel, orderStatus, paymentMethod, brandId, categoryId);

        long totalOrders = toLong(agg.get("total_orders"));
        long completedOrders = toLong(agg.get("completed_cnt"));
        BigDecimal actualRevenue = toBigDecimal(agg.get("actual_rev"));
        long provisionalOrders = toLong(agg.get("provisional_cnt"));
        BigDecimal provisionalRevenue = toBigDecimal(agg.get("provisional_rev"));
        long cancelledOrders = toLong(agg.get("cancelled_cnt"));
        BigDecimal cancelledValue = toBigDecimal(agg.get("cancelled_val"));

        BigDecimal aov = completedOrders > 0
                ? actualRevenue.divide(BigDecimal.valueOf(completedOrders), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        double successRate = totalOrders > 0 ? round1((completedOrders * 100.0) / totalOrders) : 0.0;
        double cancelRate = totalOrders > 0 ? round1((cancelledOrders * 100.0) / totalOrders) : 0.0;

        // Previous period revenue comparison (% growth)
        BigDecimal prevRevenue = BigDecimal.ZERO;
        Double growthPct = null;
        if (fromDate != null && toDate != null && !toDate.isBefore(fromDate)) {
            long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
            LocalDate prevEnd = fromDate.minusDays(1);
            LocalDate prevStart = prevEnd.minusDays(days - 1);
            prevRevenue = reportQueryRepository.queryActualRevenue(
                    prevStart.atStartOfDay(), prevEnd.atTime(LocalTime.MAX),
                    channel, paymentMethod, brandId, categoryId);
            if (prevRevenue.compareTo(BigDecimal.ZERO) > 0) {
                growthPct = actualRevenue.subtract(prevRevenue)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(prevRevenue, 1, RoundingMode.HALF_UP)
                        .doubleValue();
            } else if (actualRevenue.compareTo(BigDecimal.ZERO) > 0) {
                growthPct = 100.0;
            } else {
                growthPct = 0.0;
            }
        }

        // Funnel stages
        long fConf = toLong(agg.get("funnel_confirmed"));
        BigDecimal fConfAmt = toBigDecimal(agg.get("funnel_confirmed_amt"));
        long fShip = toLong(agg.get("funnel_shipping"));
        BigDecimal fShipAmt = toBigDecimal(agg.get("funnel_shipping_amt"));
        BigDecimal totalPlacedAmt = toBigDecimal(agg.get("total_placed_amt"));

        List<DashboardResponse.FunnelStageDto> funnel = List.of(
                DashboardResponse.FunnelStageDto.builder()
                        .stageKey("PLACED").label("1. Khách đặt đơn")
                        .count(totalOrders).amount(totalPlacedAmt)
                        .rateFromTotal(totalOrders > 0 ? 100.0 : 0.0).build(),
                DashboardResponse.FunnelStageDto.builder()
                        .stageKey("CONFIRMED").label("2. Cửa hàng xác nhận")
                        .count(fConf).amount(fConfAmt)
                        .rateFromTotal(totalOrders > 0 ? round1((fConf * 100.0) / totalOrders) : 0.0).build(),
                DashboardResponse.FunnelStageDto.builder()
                        .stageKey("SHIPPING").label("3. Xuất kho / Giao hàng")
                        .count(fShip).amount(fShipAmt)
                        .rateFromTotal(totalOrders > 0 ? round1((fShip * 100.0) / totalOrders) : 0.0).build(),
                DashboardResponse.FunnelStageDto.builder()
                        .stageKey("COMPLETED").label("4. Giao thành công & Thu tiền")
                        .count(completedOrders).amount(actualRevenue)
                        .rateFromTotal(successRate).build(),
                DashboardResponse.FunnelStageDto.builder()
                        .stageKey("CANCELLED").label("Nhánh rơi rụng: Đã hủy / Hoàn")
                        .count(cancelledOrders).amount(cancelledValue)
                        .rateFromTotal(cancelRate).build()
        );

        List<DashboardResponse.RevenuePointDto> trend = reportQueryRepository.queryRevenueSeries(
                cleanGroup, start, end, channel, orderStatus, paymentMethod, brandId, categoryId);
        List<DashboardResponse.PaymentMethodStatDto> paymentStats = reportQueryRepository.queryPaymentMethodStats(
                start, end, channel, orderStatus, paymentMethod, brandId, categoryId);
        List<DashboardResponse.ChannelComparisonDto> channelComp = reportQueryRepository.queryChannelComparison(
                start, end, channel, orderStatus, paymentMethod, brandId, categoryId);
        List<DashboardResponse.StatusCountDto> statusCounts = reportQueryRepository.queryOrderStatusCounts(
                start, end, channel, orderStatus, paymentMethod, brandId, categoryId);
        List<DashboardResponse.ProductSalesStatDto> topProducts = reportQueryRepository.queryProductSalesStats(
                start, end, channel, orderStatus, paymentMethod, brandId, categoryId, true, 5);
        List<DashboardResponse.LowStockAlertDto> lowStock = reportQueryRepository.queryLowStockAlerts(
                5, brandId, categoryId, 12);

        return DashboardResponse.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .totalCustomers(totalCustomers)
                .totalEmployees(totalEmployees)
                .totalOrders(totalOrders)
                .pendingOrders(provisionalOrders)
                .todayRevenue(todayRevenue)
                .monthRevenue(monthRevenue)
                .totalRevenue(allTimeRevenue)
                .actualRevenue(actualRevenue)
                .previousActualRevenue(prevRevenue)
                .revenueGrowthPercent(growthPct)
                .provisionalRevenue(provisionalRevenue)
                .provisionalOrderCount(provisionalOrders)
                .cancelledValue(cancelledValue)
                .cancelledOrderCount(cancelledOrders)
                .completedOrderCount(completedOrders)
                .aov(aov)
                .deliverySuccessRate(successRate)
                .cancellationRate(cancelRate)
                .revenueTrend(trend)
                .paymentMethodStats(paymentStats)
                .channelComparison(channelComp)
                .orderFunnel(funnel)
                .topSellingProducts(topProducts)
                .lowStockAlerts(lowStock)
                .revenueByDay(trend)
                .revenueByMonth(Collections.emptyList())
                .revenueByYear(Collections.emptyList())
                .orderStatusCounts(statusCounts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ComprehensiveReportResponse getComprehensiveReport(
            String groupBy, LocalDate fromDate, LocalDate toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId) {

        LocalDateTime start = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime end = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
        String cleanGroup = (groupBy != null && !groupBy.isBlank()) ? groupBy.toUpperCase() : "DAY";

        Map<String, Object> agg = reportQueryRepository.queryOrderAggregateSummary(
                start, end, channel, orderStatus, paymentMethod, brandId, categoryId);
        long totalOrders = toLong(agg.get("total_orders"));
        long completedOrders = toLong(agg.get("completed_cnt"));
        long cancelledOrders = toLong(agg.get("cancelled_cnt"));
        BigDecimal provisionalRevenue = toBigDecimal(agg.get("provisional_rev"));
        BigDecimal cancelledValue = toBigDecimal(agg.get("cancelled_val"));

        List<DashboardResponse.RevenuePointDto> revenueSeries = reportQueryRepository.queryRevenueSeries(
                cleanGroup, start, end, channel, orderStatus, paymentMethod, brandId, categoryId);

        BigDecimal totalRevenue = revenueSeries.stream().map(DashboardResponse.RevenuePointDto::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPreTax = revenueSeries.stream().map(DashboardResponse.RevenuePointDto::getPreTaxRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVat = revenueSeries.stream().map(DashboardResponse.RevenuePointDto::getVatAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCash = revenueSeries.stream().map(DashboardResponse.RevenuePointDto::getCashRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalBank = revenueSeries.stream().map(DashboardResponse.RevenuePointDto::getBankTransferRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCod = revenueSeries.stream().map(DashboardResponse.RevenuePointDto::getCodRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalQtySold = revenueSeries.stream().mapToLong(DashboardResponse.RevenuePointDto::getQuantitySold).sum();

        BigDecimal aov = completedOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(completedOrders), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        double successRate = totalOrders > 0 ? round1((completedOrders * 100.0) / totalOrders) : 0.0;
        double cancelRate = totalOrders > 0 ? round1((cancelledOrders * 100.0) / totalOrders) : 0.0;

        List<DashboardResponse.PaymentMethodStatDto> paymentStats = reportQueryRepository.queryPaymentMethodStats(
                start, end, channel, orderStatus, paymentMethod, brandId, categoryId);
        List<DashboardResponse.ChannelComparisonDto> channelComparison = reportQueryRepository.queryChannelComparison(
                start, end, channel, orderStatus, paymentMethod, brandId, categoryId);
        List<DashboardResponse.StatusCountDto> orderStatusStats = reportQueryRepository.queryOrderStatusCounts(
                start, end, channel, orderStatus, paymentMethod, brandId, categoryId);

        List<ComprehensiveReportResponse.CancelledOrderItemDto> cancelledList = reportQueryRepository.queryCancelledOrders(
                start, end, channel, paymentMethod, brandId, categoryId);
        List<ComprehensiveReportResponse.CancelReasonStatDto> cancelReasons = aggregateCancelReasons(cancelledList);

        List<DashboardResponse.ProductSalesStatDto> topSelling = reportQueryRepository.queryProductSalesStats(
                start, end, channel, orderStatus, paymentMethod, brandId, categoryId, true, 10);
        List<DashboardResponse.ProductSalesStatDto> slowSelling = reportQueryRepository.queryProductSalesStats(
                start, end, channel, orderStatus, paymentMethod, brandId, categoryId, false, 10);
        List<ComprehensiveReportResponse.SizeSalesStatDto> sizeStats = reportQueryRepository.querySizeStats(
                start, end, channel, orderStatus, paymentMethod, brandId, categoryId);
        List<ComprehensiveReportResponse.ColorSalesStatDto> colorStats = reportQueryRepository.queryColorStats(
                start, end, channel, orderStatus, paymentMethod, brandId, categoryId);
        List<ComprehensiveReportResponse.BrandSalesStatDto> brandStats = reportQueryRepository.queryBrandStats(
                start, end, channel, orderStatus, paymentMethod, brandId, categoryId);

        List<ComprehensiveReportResponse.EmployeeSalesStatDto> employeeStats = reportQueryRepository.queryEmployeeStats(
                start, end, paymentMethod, brandId, categoryId);
        List<ComprehensiveReportResponse.CustomerSalesStatDto> customerStats = reportQueryRepository.queryCustomerStats(
                start, end, channel, paymentMethod);
        List<ComprehensiveReportResponse.PurchaseSupplierStatDto> purchaseStats = reportQueryRepository.queryPurchaseStats(
                start, end);

        BigDecimal totalPurchaseAmount = purchaseStats.stream()
                .map(ComprehensiveReportResponse.PurchaseSupplierStatDto::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalConfirmedPurchases = purchaseStats.stream()
                .mapToLong(ComprehensiveReportResponse.PurchaseSupplierStatDto::getConfirmedCount)
                .sum();

        String filterSummary = String.format("Kỳ: %s → %s | Chu kỳ: %s | Kênh: %s | Trạng thái: %s | Thanh toán: %s",
                fromDate != null ? fromDate : "Toàn thời gian",
                toDate != null ? toDate : "Hiện tại",
                cleanGroup,
                (channel == null || channel.isBlank()) ? "Tất cả" : channel,
                (orderStatus == null || orderStatus.isBlank()) ? "Tất cả" : orderStatus,
                (paymentMethod == null || paymentMethod.isBlank()) ? "Tất cả" : paymentMethod);

        return ComprehensiveReportResponse.builder()
                .filterSummary(filterSummary)
                .totalRevenue(totalRevenue)
                .totalPreTaxRevenue(totalPreTax)
                .totalVatAmount(totalVat)
                .totalCashRevenue(totalCash)
                .totalBankTransferRevenue(totalBank)
                .totalCodRevenue(totalCod)
                .provisionalRevenue(provisionalRevenue)
                .cancelledValue(cancelledValue)
                .totalOrders(totalOrders)
                .totalCompletedOrders(completedOrders)
                .totalCancelledOrders(cancelledOrders)
                .totalQuantitySold(totalQtySold)
                .aov(aov)
                .deliverySuccessRate(successRate)
                .cancellationRate(cancelRate)
                .totalPurchaseAmount(totalPurchaseAmount)
                .totalConfirmedPurchases(totalConfirmedPurchases)
                .revenueSeries(revenueSeries)
                .paymentMethodStats(paymentStats)
                .channelComparison(channelComparison)
                .orderStatusStats(orderStatusStats)
                .cancelReasonStats(cancelReasons)
                .cancelledOrders(cancelledList)
                .topSellingProducts(topSelling)
                .slowSellingProducts(slowSelling)
                .sizeStats(sizeStats)
                .colorStats(colorStats)
                .brandStats(brandStats)
                .employeeRevenueStats(employeeStats)
                .customerStats(customerStats)
                .purchaseStats(purchaseStats)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReportToExcel(
            String groupBy, LocalDate fromDate, LocalDate toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId) {
        ComprehensiveReportResponse report = getComprehensiveReport(
                groupBy, fromDate, toDate, channel, orderStatus, paymentMethod, brandId, categoryId);
        return reportExcelExporter.exportToExcel(report);
    }

    private List<ComprehensiveReportResponse.CancelReasonStatDto> aggregateCancelReasons(
            List<ComprehensiveReportResponse.CancelledOrderItemDto> list) {
        Map<String, ComprehensiveReportResponse.CancelReasonStatDto> map = new LinkedHashMap<>();
        String[] buckets = {
                "Khách đổi ý",
                "Sai thông tin địa chỉ / SĐT",
                "Không liên lạc được khi giao hàng",
                "Khách từ chối nhận (Không ưng mẫu / size)",
                "Lý do khác / Nội bộ"
        };
        for (String b : buckets) {
            map.put(b, ComprehensiveReportResponse.CancelReasonStatDto.builder()
                    .reasonCategory(b).count(0).lostValue(BigDecimal.ZERO).percentage(0.0).build());
        }
        for (ComprehensiveReportResponse.CancelledOrderItemDto item : list) {
            String cat = item.getReasonCategory() != null ? item.getReasonCategory() : "Lý do khác / Nội bộ";
            ComprehensiveReportResponse.CancelReasonStatDto stat = map.computeIfAbsent(cat, k ->
                    ComprehensiveReportResponse.CancelReasonStatDto.builder()
                            .reasonCategory(k).count(0).lostValue(BigDecimal.ZERO).percentage(0.0).build());
            stat.setCount(stat.getCount() + 1);
            stat.setLostValue(stat.getLostValue().add(nvl(item.getAmount())));
        }
        long total = list.size();
        for (ComprehensiveReportResponse.CancelReasonStatDto s : map.values()) {
            s.setPercentage(total > 0 ? round1((s.getCount() * 100.0) / total) : 0.0);
        }
        return new ArrayList<>(map.values());
    }

    private long toLong(Object v) {
        return v instanceof Number n ? n.longValue() : 0L;
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
