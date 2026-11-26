package com.sneakerstore.repository.impl;

import com.sneakerstore.dto.response.ComprehensiveReportResponse;
import com.sneakerstore.dto.response.DashboardResponse;
import com.sneakerstore.repository.ReportQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class ReportQueryRepositoryImpl implements ReportQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    // Effective payment expression: Online + CASH => COD
    private static final String EFF_PAYMENT_SQL =
            "CASE " +
            "  WHEN UPPER(so.PAYMENT_METHOD) = 'BANK_TRANSFER' THEN 'BANK_TRANSFER' " +
            "  WHEN UPPER(so.PAYMENT_METHOD) = 'COD' OR (UPPER(so.PAYMENT_METHOD) = 'CASH' AND so.EMPLOYEE_ID IS NULL) THEN 'COD' " +
            "  ELSE 'CASH' " +
            "END";

    private static final String EFF_CHANNEL_SQL =
            "CASE WHEN so.EMPLOYEE_ID IS NULL THEN 'ONLINE' ELSE 'POS' END";

    private void appendOrderFilters(
            StringBuilder sql, MapSqlParameterSource params,
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId, boolean joinedProduct) {
        if (fromDate != null) {
            sql.append(" AND so.ORDER_DATE >= :fromDate ");
            params.addValue("fromDate", Timestamp.valueOf(fromDate));
        }
        if (toDate != null) {
            sql.append(" AND so.ORDER_DATE <= :toDate ");
            params.addValue("toDate", Timestamp.valueOf(toDate));
        }
        if (channel != null && !channel.isBlank() && !"ALL".equalsIgnoreCase(channel)) {
            if ("ONLINE".equalsIgnoreCase(channel)) {
                sql.append(" AND so.EMPLOYEE_ID IS NULL ");
            } else if ("POS".equalsIgnoreCase(channel)) {
                sql.append(" AND so.EMPLOYEE_ID IS NOT NULL ");
            }
        }
        if (orderStatus != null && !orderStatus.isBlank() && !"ALL".equalsIgnoreCase(orderStatus)) {
            String st = orderStatus.trim().toUpperCase();
            if ("PROCESSING".equals(st)) {
                sql.append(" AND so.STATUS IN ('DRAFT', 'PENDING', 'CONFIRMED', 'SHIPPING') ");
            } else {
                sql.append(" AND so.STATUS = :orderStatus ");
                params.addValue("orderStatus", st);
            }
        }
        if (paymentMethod != null && !paymentMethod.isBlank() && !"ALL".equalsIgnoreCase(paymentMethod)) {
            String pm = paymentMethod.trim().toUpperCase();
            if ("COD".equals(pm)) {
                sql.append(" AND (UPPER(so.PAYMENT_METHOD) = 'COD' OR (UPPER(so.PAYMENT_METHOD) = 'CASH' AND so.EMPLOYEE_ID IS NULL)) ");
            } else if ("CASH".equals(pm)) {
                sql.append(" AND UPPER(so.PAYMENT_METHOD) = 'CASH' AND so.EMPLOYEE_ID IS NOT NULL ");
            } else if ("BANK_TRANSFER".equals(pm)) {
                sql.append(" AND UPPER(so.PAYMENT_METHOD) = 'BANK_TRANSFER' ");
            }
        }
        if (brandId != null || categoryId != null) {
            if (joinedProduct) {
                if (brandId != null) {
                    sql.append(" AND p.BRAND_ID = :brandId ");
                    params.addValue("brandId", brandId);
                }
                if (categoryId != null) {
                    sql.append(" AND p.CATEGORY_ID = :categoryId ");
                    params.addValue("categoryId", categoryId);
                }
            } else {
                sql.append(" AND EXISTS (SELECT 1 FROM SALES_ORDER_DETAILS sod_f ")
                   .append(" JOIN PRODUCT_VARIANTS pv_f ON pv_f.ID = sod_f.PRODUCT_VARIANT_ID ")
                   .append(" JOIN PRODUCTS p_f ON p_f.ID = pv_f.PRODUCT_ID ")
                   .append(" WHERE sod_f.SALES_ORDER_ID = so.ID ");
                if (brandId != null) {
                    sql.append(" AND p_f.BRAND_ID = :brandId ");
                    params.addValue("brandId", brandId);
                }
                if (categoryId != null) {
                    sql.append(" AND p_f.CATEGORY_ID = :categoryId ");
                    params.addValue("categoryId", categoryId);
                }
                sql.append(") ");
            }
        }
    }

    private String periodExpr(String groupBy) {
        return switch (groupBy != null ? groupBy.toUpperCase() : "DAY") {
            case "WEEK" -> "TO_CHAR(so.ORDER_DATE, 'IYYY-\"W\"IW')";
            case "MONTH" -> "TO_CHAR(so.ORDER_DATE, 'YYYY-MM')";
            case "QUARTER" -> "TO_CHAR(so.ORDER_DATE, 'YYYY-\"Q\"Q')";
            case "YEAR" -> "TO_CHAR(so.ORDER_DATE, 'YYYY')";
            default -> "TO_CHAR(so.ORDER_DATE, 'YYYY-MM-DD')";
        };
    }

    @Override
    public List<DashboardResponse.RevenuePointDto> queryRevenueSeries(
            String groupBy, LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId) {
        String pExpr = periodExpr(groupBy);
        MapSqlParameterSource params = new MapSqlParameterSource();

        // Mặc định doanh thu thực nhận tính theo COMPLETED; nếu user lọc trạng thái khác (VD PROCESSING / CANCELLED) thì theo lọc đó
        String targetStatusClause = (orderStatus == null || orderStatus.isBlank() || "ALL".equalsIgnoreCase(orderStatus))
                ? " AND so.STATUS = 'COMPLETED' "
                : "";

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(pExpr).append(" AS period, ")
           .append("COUNT(DISTINCT so.ID) AS order_count, ")
           .append("COALESCE(SUM(sod.QUANTITY), 0) AS qty_sold, ")
           .append("COALESCE(SUM(sod.SUBTOTAL), 0) AS revenue, ")
           .append("COALESCE(SUM(CASE WHEN (").append(EFF_PAYMENT_SQL).append(") = 'CASH' THEN sod.SUBTOTAL ELSE 0 END), 0) AS cash_rev, ")
           .append("COALESCE(SUM(CASE WHEN (").append(EFF_PAYMENT_SQL).append(") = 'BANK_TRANSFER' THEN sod.SUBTOTAL ELSE 0 END), 0) AS bank_rev, ")
           .append("COALESCE(SUM(CASE WHEN (").append(EFF_PAYMENT_SQL).append(") = 'COD' THEN sod.SUBTOTAL ELSE 0 END), 0) AS cod_rev ")
           .append("FROM SALES_ORDERS so ")
           .append("JOIN SALES_ORDER_DETAILS sod ON sod.SALES_ORDER_ID = so.ID ")
           .append("JOIN PRODUCT_VARIANTS pv ON pv.ID = sod.PRODUCT_VARIANT_ID ")
           .append("JOIN PRODUCTS p ON p.ID = pv.PRODUCT_ID ")
           .append("WHERE 1=1 ")
           .append(targetStatusClause);

        appendOrderFilters(sql, params, fromDate, toDate, channel, orderStatus, paymentMethod, brandId, categoryId, true);

        sql.append("GROUP BY ").append(pExpr).append(" ORDER BY period ASC");

        // Lấy thêm tiền thuế VAT thực tế từ bảng INVOICES (nếu không lọc brand/category) để đối soát khớp 100% module Hóa đơn
        Map<String, BigDecimal[]> invoiceTaxByPeriod = new HashMap<>();
        if (brandId == null && categoryId == null) {
            StringBuilder invSql = new StringBuilder();
            invSql.append("SELECT ").append(pExpr).append(" AS period, ")
                  .append("COALESCE(SUM(inv.SUBTOTAL), 0) AS inv_sub, ")
                  .append("COALESCE(SUM(inv.TAX_AMOUNT), 0) AS inv_tax ")
                  .append("FROM INVOICES inv ")
                  .append("JOIN SALES_ORDERS so ON so.ID = inv.ORDER_ID ")
                  .append("WHERE inv.INVOICE_STATUS <> 'CANCELLED' AND inv.RECORD_KIND = 'ORIGINAL' ")
                  .append(targetStatusClause);
            MapSqlParameterSource invParams = new MapSqlParameterSource();
            appendOrderFilters(invSql, invParams, fromDate, toDate, channel, orderStatus, paymentMethod, null, null, false);
            invSql.append("GROUP BY ").append(pExpr);
            jdbcTemplate.query(invSql.toString(), invParams, rs -> {
                invoiceTaxByPeriod.put(rs.getString("period"),
                        new BigDecimal[]{nvl(rs.getBigDecimal("inv_sub")), nvl(rs.getBigDecimal("inv_tax"))});
            });
        }

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            String period = rs.getString("period");
            BigDecimal rev = nvl(rs.getBigDecimal("revenue"));
            BigDecimal preTax;
            BigDecimal vat;
            BigDecimal[] invPair = invoiceTaxByPeriod.get(period);
            if (invPair != null && invPair[0].add(invPair[1]).compareTo(BigDecimal.ZERO) > 0) {
                preTax = invPair[0];
                vat = invPair[1];
            } else {
                // Tách VAT mặc định 8% đã gồm trong giá bán để luôn minh bạch tiền hàng & VAT
                preTax = rev.divide(BigDecimal.valueOf(1.08), 0, RoundingMode.HALF_UP);
                vat = rev.subtract(preTax);
            }
            return DashboardResponse.RevenuePointDto.builder()
                    .period(period)
                    .orderCount(rs.getLong("order_count"))
                    .quantitySold(rs.getLong("qty_sold"))
                    .preTaxRevenue(preTax)
                    .vatAmount(vat)
                    .cashRevenue(nvl(rs.getBigDecimal("cash_rev")))
                    .bankTransferRevenue(nvl(rs.getBigDecimal("bank_rev")))
                    .codRevenue(nvl(rs.getBigDecimal("cod_rev")))
                    .revenue(rev)
                    .build();
        });
    }

    @Override
    public Map<String, Object> queryOrderAggregateSummary(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
           .append("COUNT(*) AS total_orders, ")
           .append("COALESCE(SUM(CASE WHEN so.STATUS = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completed_cnt, ")
           .append("COALESCE(SUM(CASE WHEN so.STATUS = 'COMPLETED' THEN so.FINAL_AMOUNT ELSE 0 END), 0) AS actual_rev, ")
           .append("COALESCE(SUM(CASE WHEN so.STATUS IN ('DRAFT','PENDING','CONFIRMED','SHIPPING') THEN 1 ELSE 0 END), 0) AS provisional_cnt, ")
           .append("COALESCE(SUM(CASE WHEN so.STATUS IN ('DRAFT','PENDING','CONFIRMED','SHIPPING') THEN so.FINAL_AMOUNT ELSE 0 END), 0) AS provisional_rev, ")
           .append("COALESCE(SUM(CASE WHEN so.STATUS = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS cancelled_cnt, ")
           .append("COALESCE(SUM(CASE WHEN so.STATUS = 'CANCELLED' THEN so.FINAL_AMOUNT ELSE 0 END), 0) AS cancelled_val, ")
           .append("COALESCE(SUM(CASE WHEN so.STATUS IN ('CONFIRMED','SHIPPING','COMPLETED') THEN 1 ELSE 0 END), 0) AS funnel_confirmed, ")
           .append("COALESCE(SUM(CASE WHEN so.STATUS IN ('CONFIRMED','SHIPPING','COMPLETED') THEN so.FINAL_AMOUNT ELSE 0 END), 0) AS funnel_confirmed_amt, ")
           .append("COALESCE(SUM(CASE WHEN so.STATUS IN ('SHIPPING','COMPLETED') THEN 1 ELSE 0 END), 0) AS funnel_shipping, ")
           .append("COALESCE(SUM(CASE WHEN so.STATUS IN ('SHIPPING','COMPLETED') THEN so.FINAL_AMOUNT ELSE 0 END), 0) AS funnel_shipping_amt, ")
           .append("COALESCE(SUM(so.FINAL_AMOUNT), 0) AS total_placed_amt ")
           .append("FROM SALES_ORDERS so WHERE 1=1 ");

        appendOrderFilters(sql, params, fromDate, toDate, channel, orderStatus, paymentMethod, brandId, categoryId, false);
        return jdbcTemplate.queryForMap(sql.toString(), params);
    }

    @Override
    public BigDecimal queryActualRevenue(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String paymentMethod,
            Long brandId, Long categoryId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(so.FINAL_AMOUNT), 0) FROM SALES_ORDERS so WHERE so.STATUS = 'COMPLETED' ");
        appendOrderFilters(sql, params, fromDate, toDate, channel, null, paymentMethod, brandId, categoryId, false);
        BigDecimal val = jdbcTemplate.queryForObject(sql.toString(), params, BigDecimal.class);
        return nvl(val);
    }

    @Override
    public List<DashboardResponse.PaymentMethodStatDto> queryPaymentMethodStats(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String statusClause = (orderStatus == null || orderStatus.isBlank() || "ALL".equalsIgnoreCase(orderStatus))
                ? " AND so.STATUS = 'COMPLETED' "
                : "";
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT (").append(EFF_PAYMENT_SQL).append(") AS pm, ")
           .append("COUNT(*) AS cnt, ")
           .append("COALESCE(SUM(so.FINAL_AMOUNT), 0) AS amt ")
           .append("FROM SALES_ORDERS so WHERE 1=1 ")
           .append(statusClause);
        appendOrderFilters(sql, params, fromDate, toDate, channel, orderStatus, paymentMethod, brandId, categoryId, false);
        sql.append("GROUP BY (").append(EFF_PAYMENT_SQL).append(")");

        Map<String, DashboardResponse.PaymentMethodStatDto> map = new LinkedHashMap<>();
        map.put("BANK_TRANSFER", DashboardResponse.PaymentMethodStatDto.builder()
                .paymentMethod("BANK_TRANSFER").label("Chuyển khoản (SePay)").orderCount(0).amount(BigDecimal.ZERO).percentage(0.0).build());
        map.put("CASH", DashboardResponse.PaymentMethodStatDto.builder()
                .paymentMethod("CASH").label("Tiền mặt (Tại quầy)").orderCount(0).amount(BigDecimal.ZERO).percentage(0.0).build());
        map.put("COD", DashboardResponse.PaymentMethodStatDto.builder()
                .paymentMethod("COD").label("Thu hộ (COD)").orderCount(0).amount(BigDecimal.ZERO).percentage(0.0).build());

        jdbcTemplate.query(sql.toString(), params, rs -> {
            String pm = rs.getString("pm");
            DashboardResponse.PaymentMethodStatDto item = map.get(pm);
            if (item != null) {
                item.setOrderCount(rs.getLong("cnt"));
                item.setAmount(nvl(rs.getBigDecimal("amt")));
            }
        });

        BigDecimal total = map.values().stream()
                .map(DashboardResponse.PaymentMethodStatDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (DashboardResponse.PaymentMethodStatDto item : map.values()) {
            double pct = total.compareTo(BigDecimal.ZERO) > 0
                    ? item.getAmount().multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            item.setPercentage(pct);
        }
        return new ArrayList<>(map.values());
    }

    @Override
    public List<DashboardResponse.ChannelComparisonDto> queryChannelComparison(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT (").append(EFF_CHANNEL_SQL).append(") AS ch, ")
           .append("COUNT(*) AS total_orders, ")
           .append("COALESCE(SUM(CASE WHEN so.STATUS = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completed_orders, ")
           .append("COALESCE(SUM(CASE WHEN so.STATUS = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS cancelled_orders, ")
           .append("COALESCE(SUM(CASE WHEN so.STATUS IN ('DRAFT','PENDING','CONFIRMED','SHIPPING') THEN 1 ELSE 0 END), 0) AS processing_orders, ")
           .append("COALESCE(SUM(CASE WHEN so.STATUS = 'COMPLETED' THEN so.FINAL_AMOUNT ELSE 0 END), 0) AS actual_rev, ")
           .append("COALESCE(SUM(CASE WHEN so.STATUS = 'CANCELLED' THEN so.FINAL_AMOUNT ELSE 0 END), 0) AS cancelled_val, ")
           .append("COALESCE(SUM(COALESCE(inv.SHIPPING_FEE, 0)), 0) AS ship_fee ")
           .append("FROM SALES_ORDERS so ")
           .append("LEFT JOIN INVOICES inv ON inv.ORDER_ID = so.ID AND inv.RECORD_KIND = 'ORIGINAL' AND inv.INVOICE_STATUS <> 'CANCELLED' ")
           .append("WHERE 1=1 ");
        appendOrderFilters(sql, params, fromDate, toDate, channel, orderStatus, paymentMethod, brandId, categoryId, false);
        sql.append("GROUP BY (").append(EFF_CHANNEL_SQL).append(")");

        Map<String, DashboardResponse.ChannelComparisonDto> map = new LinkedHashMap<>();
        map.put("POS", DashboardResponse.ChannelComparisonDto.builder()
                .channel("POS").label("Bán tại quầy (POS)")
                .actualRevenue(BigDecimal.ZERO).cancelledValue(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO).aov(BigDecimal.ZERO)
                .successRate(0.0).cancellationRate(0.0).instantPaymentRate(100.0).revenueSharePercent(0.0).build());
        map.put("ONLINE", DashboardResponse.ChannelComparisonDto.builder()
                .channel("ONLINE").label("Bán trực tuyến (Online Store)")
                .actualRevenue(BigDecimal.ZERO).cancelledValue(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO).aov(BigDecimal.ZERO)
                .successRate(0.0).cancellationRate(0.0).instantPaymentRate(0.0).revenueSharePercent(0.0).build());

        jdbcTemplate.query(sql.toString(), params, rs -> {
            String ch = rs.getString("ch");
            DashboardResponse.ChannelComparisonDto dto = map.get(ch);
            if (dto != null) {
                long tot = rs.getLong("total_orders");
                long comp = rs.getLong("completed_orders");
                long canc = rs.getLong("cancelled_orders");
                long proc = rs.getLong("processing_orders");
                BigDecimal rev = nvl(rs.getBigDecimal("actual_rev"));
                dto.setTotalOrders(tot);
                dto.setCompletedOrders(comp);
                dto.setCancelledOrders(canc);
                dto.setProcessingOrders(proc);
                dto.setActualRevenue(rev);
                dto.setCancelledValue(nvl(rs.getBigDecimal("cancelled_val")));
                dto.setShippingFee("POS".equals(ch) ? BigDecimal.ZERO : nvl(rs.getBigDecimal("ship_fee")));
                dto.setAov(comp > 0 ? rev.divide(BigDecimal.valueOf(comp), 0, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                dto.setSuccessRate(tot > 0 ? round1((comp * 100.0) / tot) : 0.0);
                dto.setCancellationRate(tot > 0 ? round1((canc * 100.0) / tot) : 0.0);
                dto.setInstantPaymentRate("POS".equals(ch) ? 100.0 : (tot > 0 ? round1((comp * 100.0) / tot) : 0.0));
            }
        });

        BigDecimal totalRev = map.values().stream()
                .map(DashboardResponse.ChannelComparisonDto::getActualRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (DashboardResponse.ChannelComparisonDto dto : map.values()) {
            double share = totalRev.compareTo(BigDecimal.ZERO) > 0
                    ? dto.getActualRevenue().multiply(BigDecimal.valueOf(100)).divide(totalRev, 1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            dto.setRevenueSharePercent(share);
        }
        return new ArrayList<>(map.values());
    }

    @Override
    public List<DashboardResponse.StatusCountDto> queryOrderStatusCounts(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder(
                "SELECT so.STATUS AS status, COUNT(*) AS cnt, COALESCE(SUM(so.FINAL_AMOUNT), 0) AS total_amount " +
                "FROM SALES_ORDERS so WHERE 1=1 ");
        appendOrderFilters(sql, params, fromDate, toDate, channel, orderStatus, paymentMethod, brandId, categoryId, false);
        sql.append("GROUP BY so.STATUS ORDER BY cnt DESC");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> DashboardResponse.StatusCountDto.builder()
                .status(rs.getString("status"))
                .count(rs.getLong("cnt"))
                .totalAmount(nvl(rs.getBigDecimal("total_amount")))
                .build());
    }

    @Override
    public List<DashboardResponse.ProductSalesStatDto> queryProductSalesStats(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId, boolean topSelling, int limit) {
        String orderClause = topSelling
                ? "ORDER BY qty_sold DESC, revenue DESC, p.ID ASC"
                : "ORDER BY qty_sold ASC, revenue ASC, p.ID ASC";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("limit", limit);
        StringBuilder joinCond = new StringBuilder("so.ID = sod.SALES_ORDER_ID ");
        if (orderStatus == null || orderStatus.isBlank() || "ALL".equalsIgnoreCase(orderStatus)) {
            joinCond.append("AND so.STATUS = 'COMPLETED' ");
        } else if ("PROCESSING".equalsIgnoreCase(orderStatus)) {
            joinCond.append("AND so.STATUS IN ('DRAFT','PENDING','CONFIRMED','SHIPPING') ");
        } else {
            joinCond.append("AND so.STATUS = :orderStatus ");
            params.addValue("orderStatus", orderStatus.trim().toUpperCase());
        }
        if (fromDate != null) {
            joinCond.append("AND so.ORDER_DATE >= :fromDate ");
            params.addValue("fromDate", Timestamp.valueOf(fromDate));
        }
        if (toDate != null) {
            joinCond.append("AND so.ORDER_DATE <= :toDate ");
            params.addValue("toDate", Timestamp.valueOf(toDate));
        }
        if ("ONLINE".equalsIgnoreCase(channel)) {
            joinCond.append("AND so.EMPLOYEE_ID IS NULL ");
        } else if ("POS".equalsIgnoreCase(channel)) {
            joinCond.append("AND so.EMPLOYEE_ID IS NOT NULL ");
        }
        if (paymentMethod != null && !paymentMethod.isBlank() && !"ALL".equalsIgnoreCase(paymentMethod)) {
            String pm = paymentMethod.trim().toUpperCase();
            if ("COD".equals(pm)) {
                joinCond.append("AND (UPPER(so.PAYMENT_METHOD) = 'COD' OR (UPPER(so.PAYMENT_METHOD) = 'CASH' AND so.EMPLOYEE_ID IS NULL)) ");
            } else if ("CASH".equals(pm)) {
                joinCond.append("AND UPPER(so.PAYMENT_METHOD) = 'CASH' AND so.EMPLOYEE_ID IS NOT NULL ");
            } else if ("BANK_TRANSFER".equals(pm)) {
                joinCond.append("AND UPPER(so.PAYMENT_METHOD) = 'BANK_TRANSFER' ");
            }
        }

        StringBuilder whereCond = new StringBuilder("WHERE 1=1 ");
        if (brandId != null) {
            whereCond.append("AND p.BRAND_ID = :brandId ");
            params.addValue("brandId", brandId);
        }
        if (categoryId != null) {
            whereCond.append("AND p.CATEGORY_ID = :categoryId ");
            params.addValue("categoryId", categoryId);
        }

        String sql = "SELECT p.ID AS product_id, p.PRODUCT_CODE AS product_code, p.PRODUCT_NAME AS product_name, " +
                "p.THUMBNAIL_URL AS thumbnail_url, b.BRAND_NAME AS brand_name, c.CATEGORY_NAME AS category_name, " +
                "COALESCE(SUM(CASE WHEN so.ID IS NOT NULL THEN sod.QUANTITY ELSE 0 END), 0) AS qty_sold, " +
                "COALESCE(SUM(CASE WHEN so.ID IS NOT NULL THEN sod.SUBTOTAL ELSE 0 END), 0) AS revenue " +
                "FROM PRODUCTS p " +
                "JOIN BRANDS b ON b.ID = p.BRAND_ID " +
                "JOIN CATEGORIES c ON c.ID = p.CATEGORY_ID " +
                "LEFT JOIN PRODUCT_VARIANTS pv ON pv.PRODUCT_ID = p.ID " +
                "LEFT JOIN SALES_ORDER_DETAILS sod ON sod.PRODUCT_VARIANT_ID = pv.ID " +
                "LEFT JOIN SALES_ORDERS so ON " + joinCond +
                whereCond +
                "GROUP BY p.ID, p.PRODUCT_CODE, p.PRODUCT_NAME, p.THUMBNAIL_URL, b.BRAND_NAME, c.CATEGORY_NAME " +
                orderClause + " LIMIT :limit";

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> DashboardResponse.ProductSalesStatDto.builder()
                .productId(rs.getLong("product_id"))
                .productCode(rs.getString("product_code"))
                .productName(rs.getString("product_name"))
                .thumbnailUrl(rs.getString("thumbnail_url"))
                .brandName(rs.getString("brand_name"))
                .categoryName(rs.getString("category_name"))
                .quantitySold(rs.getLong("qty_sold"))
                .revenue(nvl(rs.getBigDecimal("revenue")))
                .build());
    }

    @Override
    public List<DashboardResponse.LowStockAlertDto> queryLowStockAlerts(
            int threshold, Long brandId, Long categoryId, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("threshold", threshold)
                .addValue("limit", limit);
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT pv.ID AS variant_id, p.ID AS product_id, p.PRODUCT_CODE AS product_code, ")
           .append("p.PRODUCT_NAME AS product_name, p.THUMBNAIL_URL AS thumbnail_url, b.BRAND_NAME AS brand_name, ")
           .append("pv.SKU AS sku, s.SIZE_VALUE AS size_value, col.COLOR_NAME AS color_name, col.HEX_CODE AS hex_code, ")
           .append("COALESCE(pv.AVAILABLE_QUANTITY, 0) AS avail_qty ")
           .append("FROM PRODUCT_VARIANTS pv ")
           .append("JOIN PRODUCTS p ON p.ID = pv.PRODUCT_ID ")
           .append("JOIN BRANDS b ON b.ID = p.BRAND_ID ")
           .append("LEFT JOIN SIZES s ON s.ID = pv.SIZE_ID ")
           .append("LEFT JOIN COLORS col ON col.ID = pv.COLOR_ID ")
           .append("WHERE pv.STATUS = 'ACTIVE' AND p.STATUS = 'ACTIVE' AND COALESCE(pv.AVAILABLE_QUANTITY, 0) < :threshold ");
        if (brandId != null) {
            sql.append(" AND p.BRAND_ID = :brandId ");
            params.addValue("brandId", brandId);
        }
        if (categoryId != null) {
            sql.append(" AND p.CATEGORY_ID = :categoryId ");
            params.addValue("categoryId", categoryId);
        }
        sql.append("ORDER BY avail_qty ASC, p.PRODUCT_NAME ASC LIMIT :limit");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> DashboardResponse.LowStockAlertDto.builder()
                .variantId(rs.getLong("variant_id"))
                .productId(rs.getLong("product_id"))
                .productCode(rs.getString("product_code"))
                .productName(rs.getString("product_name"))
                .thumbnailUrl(rs.getString("thumbnail_url"))
                .brandName(rs.getString("brand_name"))
                .sku(rs.getString("sku"))
                .sizeValue(rs.getString("size_value"))
                .colorName(rs.getString("color_name"))
                .hexCode(rs.getString("hex_code"))
                .availableQuantity(rs.getInt("avail_qty"))
                .build());
    }

    @Override
    public List<ComprehensiveReportResponse.CancelledOrderItemDto> queryCancelledOrders(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String paymentMethod,
            Long brandId, Long categoryId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT so.ID AS order_id, so.ORDER_CODE AS order_code, (").append(EFF_CHANNEL_SQL).append(") AS ch, ")
           .append("COALESCE(c.FULL_NAME, 'Khách lẻ') AS customer_name, COALESCE(c.PHONE, '---') AS phone, ")
           .append("so.ORDER_DATE AS order_date, so.FINAL_AMOUNT AS amount, (").append(EFF_PAYMENT_SQL).append(") AS pm, ")
           .append("COALESCE((SELECT osh.NOTE FROM ORDER_STATUS_HISTORY osh WHERE osh.SALES_ORDER_ID = so.ID AND osh.TO_STATUS = 'CANCELLED' ORDER BY osh.CHANGED_AT DESC LIMIT 1), so.NOTE, 'Khách hàng / Nhân viên hủy đơn') AS cancel_note ")
           .append("FROM SALES_ORDERS so ")
           .append("LEFT JOIN CUSTOMERS c ON c.ID = so.CUSTOMER_ID ")
           .append("WHERE so.STATUS = 'CANCELLED' ");
        appendOrderFilters(sql, params, fromDate, toDate, channel, null, paymentMethod, brandId, categoryId, false);
        sql.append("ORDER BY so.ORDER_DATE DESC");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            String note = rs.getString("cancel_note");
            return ComprehensiveReportResponse.CancelledOrderItemDto.builder()
                    .orderId(rs.getLong("order_id"))
                    .orderCode(rs.getString("order_code"))
                    .channel(rs.getString("ch"))
                    .customerName(rs.getString("customer_name"))
                    .customerPhone(rs.getString("phone"))
                    .orderDate(rs.getTimestamp("order_date") != null ? rs.getTimestamp("order_date").toLocalDateTime() : null)
                    .amount(nvl(rs.getBigDecimal("amount")))
                    .paymentMethod(rs.getString("pm"))
                    .reasonCategory(classifyCancelReason(note))
                    .cancelNote(note)
                    .build();
        });
    }

    @Override
    public List<ComprehensiveReportResponse.SizeSalesStatDto> querySizeStats(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String statusClause = (orderStatus == null || orderStatus.isBlank() || "ALL".equalsIgnoreCase(orderStatus))
                ? " AND so.STATUS = 'COMPLETED' "
                : "";
        StringBuilder salesSql = new StringBuilder();
        salesSql.append("SELECT s.SIZE_VALUE AS size_value, ")
                .append("COALESCE(SUM(sod.QUANTITY), 0) AS qty_sold, ")
                .append("COALESCE(SUM(sod.SUBTOTAL), 0) AS revenue ")
                .append("FROM SALES_ORDERS so ")
                .append("JOIN SALES_ORDER_DETAILS sod ON sod.SALES_ORDER_ID = so.ID ")
                .append("JOIN PRODUCT_VARIANTS pv ON pv.ID = sod.PRODUCT_VARIANT_ID ")
                .append("JOIN PRODUCTS p ON p.ID = pv.PRODUCT_ID ")
                .append("JOIN SIZES s ON s.ID = pv.SIZE_ID ")
                .append("WHERE 1=1 ").append(statusClause);
        appendOrderFilters(salesSql, params, fromDate, toDate, channel, orderStatus, paymentMethod, brandId, categoryId, true);
        salesSql.append("GROUP BY s.SIZE_VALUE ");

        Map<String, long[]> salesMap = new LinkedHashMap<>();
        Map<String, BigDecimal> revMap = new HashMap<>();
        jdbcTemplate.query(salesSql.toString(), params, rs -> {
            String sz = rs.getString("size_value");
            salesMap.put(sz, new long[]{rs.getLong("qty_sold")});
            revMap.put(sz, nvl(rs.getBigDecimal("revenue")));
        });

        // Tồn kho hiện tại theo size (tôn trọng bộ lọc Brand/Category)
        MapSqlParameterSource stockParams = new MapSqlParameterSource();
        StringBuilder stockSql = new StringBuilder(
                "SELECT s.SIZE_VALUE AS size_value, COALESCE(SUM(pv.AVAILABLE_QUANTITY), 0) AS stock " +
                "FROM SIZES s " +
                "LEFT JOIN PRODUCT_VARIANTS pv ON pv.SIZE_ID = s.ID AND pv.STATUS = 'ACTIVE' " +
                "LEFT JOIN PRODUCTS p ON p.ID = pv.PRODUCT_ID ");
        stockSql.append("WHERE 1=1 ");
        if (brandId != null) {
            stockSql.append(" AND (p.ID IS NULL OR p.BRAND_ID = :brandId) ");
            stockParams.addValue("brandId", brandId);
        }
        if (categoryId != null) {
            stockSql.append(" AND (p.ID IS NULL OR p.CATEGORY_ID = :categoryId) ");
            stockParams.addValue("categoryId", categoryId);
        }
        stockSql.append("GROUP BY s.SIZE_VALUE ORDER BY s.SIZE_VALUE ASC");

        List<ComprehensiveReportResponse.SizeSalesStatDto> list = new ArrayList<>();
        jdbcTemplate.query(stockSql.toString(), stockParams, rs -> {
            String sz = rs.getString("size_value");
            long qty = salesMap.containsKey(sz) ? salesMap.get(sz)[0] : 0L;
            BigDecimal rev = revMap.getOrDefault(sz, BigDecimal.ZERO);
            long stock = rs.getLong("stock");
            list.add(ComprehensiveReportResponse.SizeSalesStatDto.builder()
                    .sizeValue(sz)
                    .sizeGroup(classifySizeGroup(sz))
                    .quantitySold(qty)
                    .revenue(rev)
                    .currentStock(stock)
                    .sharePercent(0.0)
                    .goldenSize(false)
                    .build());
        });

        long totalSold = list.stream().mapToLong(ComprehensiveReportResponse.SizeSalesStatDto::getQuantitySold).sum();
        for (ComprehensiveReportResponse.SizeSalesStatDto item : list) {
            item.setSharePercent(totalSold > 0 ? round1((item.getQuantitySold() * 100.0) / totalSold) : 0.0);
        }

        // Xác định Top 3 Size Vàng có doanh số bán > 0
        list.stream()
                .filter(s -> s.getQuantitySold() > 0)
                .sorted(Comparator.comparingLong(ComprehensiveReportResponse.SizeSalesStatDto::getQuantitySold).reversed())
                .limit(3)
                .forEach(s -> s.setGoldenSize(true));

        return list;
    }

    @Override
    public List<ComprehensiveReportResponse.ColorSalesStatDto> queryColorStats(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String statusClause = (orderStatus == null || orderStatus.isBlank() || "ALL".equalsIgnoreCase(orderStatus))
                ? " AND so.STATUS = 'COMPLETED' "
                : "";
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT col.COLOR_NAME AS color_name, col.HEX_CODE AS hex_code, ")
           .append("COALESCE(SUM(sod.QUANTITY), 0) AS qty_sold, COALESCE(SUM(sod.SUBTOTAL), 0) AS revenue ")
           .append("FROM SALES_ORDERS so ")
           .append("JOIN SALES_ORDER_DETAILS sod ON sod.SALES_ORDER_ID = so.ID ")
           .append("JOIN PRODUCT_VARIANTS pv ON pv.ID = sod.PRODUCT_VARIANT_ID ")
           .append("JOIN PRODUCTS p ON p.ID = pv.PRODUCT_ID ")
           .append("JOIN COLORS col ON col.ID = pv.COLOR_ID ")
           .append("WHERE 1=1 ").append(statusClause);
        appendOrderFilters(sql, params, fromDate, toDate, channel, orderStatus, paymentMethod, brandId, categoryId, true);
        sql.append("GROUP BY col.COLOR_NAME, col.HEX_CODE ORDER BY qty_sold DESC, revenue DESC");

        List<ComprehensiveReportResponse.ColorSalesStatDto> list = jdbcTemplate.query(sql.toString(), params,
                (rs, rowNum) -> ComprehensiveReportResponse.ColorSalesStatDto.builder()
                        .colorName(rs.getString("color_name"))
                        .hexCode(rs.getString("hex_code"))
                        .quantitySold(rs.getLong("qty_sold"))
                        .revenue(nvl(rs.getBigDecimal("revenue")))
                        .sharePercent(0.0)
                        .build());

        long totalQty = list.stream().mapToLong(ComprehensiveReportResponse.ColorSalesStatDto::getQuantitySold).sum();
        for (ComprehensiveReportResponse.ColorSalesStatDto c : list) {
            c.setSharePercent(totalQty > 0 ? round1((c.getQuantitySold() * 100.0) / totalQty) : 0.0);
        }
        return list;
    }

    @Override
    public List<ComprehensiveReportResponse.BrandSalesStatDto> queryBrandStats(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String orderStatus, String paymentMethod,
            Long brandId, Long categoryId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String statusClause = (orderStatus == null || orderStatus.isBlank() || "ALL".equalsIgnoreCase(orderStatus))
                ? " AND so.STATUS = 'COMPLETED' "
                : "";
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT b.ID AS brand_id, b.BRAND_NAME AS brand_name, ")
           .append("COUNT(DISTINCT p.ID) AS product_cnt, ")
           .append("COALESCE(SUM(sod.QUANTITY), 0) AS qty_sold, ")
           .append("COALESCE(SUM(sod.SUBTOTAL), 0) AS revenue ")
           .append("FROM SALES_ORDERS so ")
           .append("JOIN SALES_ORDER_DETAILS sod ON sod.SALES_ORDER_ID = so.ID ")
           .append("JOIN PRODUCT_VARIANTS pv ON pv.ID = sod.PRODUCT_VARIANT_ID ")
           .append("JOIN PRODUCTS p ON p.ID = pv.PRODUCT_ID ")
           .append("JOIN BRANDS b ON b.ID = p.BRAND_ID ")
           .append("WHERE 1=1 ").append(statusClause);
        appendOrderFilters(sql, params, fromDate, toDate, channel, orderStatus, paymentMethod, brandId, categoryId, true);
        sql.append("GROUP BY b.ID, b.BRAND_NAME ORDER BY revenue DESC, qty_sold DESC");

        List<ComprehensiveReportResponse.BrandSalesStatDto> list = jdbcTemplate.query(sql.toString(), params,
                (rs, rowNum) -> ComprehensiveReportResponse.BrandSalesStatDto.builder()
                        .brandId(rs.getLong("brand_id"))
                        .brandName(rs.getString("brand_name"))
                        .productCount(rs.getLong("product_cnt"))
                        .quantitySold(rs.getLong("qty_sold"))
                        .revenue(nvl(rs.getBigDecimal("revenue")))
                        .sharePercent(0.0)
                        .build());

        BigDecimal totalRev = list.stream()
                .map(ComprehensiveReportResponse.BrandSalesStatDto::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        for (ComprehensiveReportResponse.BrandSalesStatDto b : list) {
            double share = totalRev.compareTo(BigDecimal.ZERO) > 0
                    ? b.getRevenue().multiply(BigDecimal.valueOf(100)).divide(totalRev, 1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            b.setSharePercent(share);
        }
        return list;
    }

    @Override
    public List<ComprehensiveReportResponse.EmployeeSalesStatDto> queryEmployeeStats(
            LocalDateTime fromDate, LocalDateTime toDate,
            String paymentMethod, Long brandId, Long categoryId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder joinCond = new StringBuilder("so.EMPLOYEE_ID = e.ID AND so.STATUS = 'COMPLETED' ");
        if (fromDate != null) {
            joinCond.append("AND so.ORDER_DATE >= :fromDate ");
            params.addValue("fromDate", Timestamp.valueOf(fromDate));
        }
        if (toDate != null) {
            joinCond.append("AND so.ORDER_DATE <= :toDate ");
            params.addValue("toDate", Timestamp.valueOf(toDate));
        }
        if (paymentMethod != null && !paymentMethod.isBlank() && !"ALL".equalsIgnoreCase(paymentMethod)) {
            String pm = paymentMethod.trim().toUpperCase();
            if ("CASH".equals(pm)) {
                joinCond.append("AND UPPER(so.PAYMENT_METHOD) = 'CASH' ");
            } else if ("BANK_TRANSFER".equals(pm)) {
                joinCond.append("AND UPPER(so.PAYMENT_METHOD) = 'BANK_TRANSFER' ");
            } else if ("COD".equals(pm)) {
                joinCond.append("AND UPPER(so.PAYMENT_METHOD) = 'COD' ");
            }
        }
        if (brandId != null || categoryId != null) {
            joinCond.append("AND EXISTS (SELECT 1 FROM SALES_ORDER_DETAILS sod_e ")
                    .append("JOIN PRODUCT_VARIANTS pv_e ON pv_e.ID = sod_e.PRODUCT_VARIANT_ID ")
                    .append("JOIN PRODUCTS p_e ON p_e.ID = pv_e.PRODUCT_ID WHERE sod_e.SALES_ORDER_ID = so.ID ");
            if (brandId != null) {
                joinCond.append("AND p_e.BRAND_ID = :brandId ");
                params.addValue("brandId", brandId);
            }
            if (categoryId != null) {
                joinCond.append("AND p_e.CATEGORY_ID = :categoryId ");
                params.addValue("categoryId", categoryId);
            }
            joinCond.append(") ");
        }

        String sql = "SELECT e.ID AS employee_id, e.EMPLOYEE_CODE AS employee_code, e.FULL_NAME AS employee_name, " +
                "COUNT(DISTINCT DATE(so.ORDER_DATE)) AS working_shifts, " +
                "COUNT(so.ID) AS completed_cnt, " +
                "COALESCE(SUM(so.FINAL_AMOUNT), 0) AS total_rev " +
                "FROM EMPLOYEES e " +
                "LEFT JOIN SALES_ORDERS so ON " + joinCond +
                "GROUP BY e.ID, e.EMPLOYEE_CODE, e.FULL_NAME " +
                "ORDER BY total_rev DESC, e.ID ASC";

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            long comp = rs.getLong("completed_cnt");
            BigDecimal rev = nvl(rs.getBigDecimal("total_rev"));
            BigDecimal avg = comp > 0 ? rev.divide(BigDecimal.valueOf(comp), 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            return ComprehensiveReportResponse.EmployeeSalesStatDto.builder()
                    .employeeId(rs.getLong("employee_id"))
                    .employeeCode(rs.getString("employee_code"))
                    .employeeName(rs.getString("employee_name"))
                    .workingShifts(rs.getLong("working_shifts"))
                    .completedOrderCount(comp)
                    .totalRevenue(rev)
                    .averageOrderValue(avg)
                    .build();
        });
    }

    @Override
    public List<ComprehensiveReportResponse.CustomerSalesStatDto> queryCustomerStats(
            LocalDateTime fromDate, LocalDateTime toDate,
            String channel, String paymentMethod) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder joinCond = new StringBuilder("so.CUSTOMER_ID = c.ID AND so.STATUS = 'COMPLETED' ");
        if (fromDate != null) {
            joinCond.append("AND so.ORDER_DATE >= :fromDate ");
            params.addValue("fromDate", Timestamp.valueOf(fromDate));
        }
        if (toDate != null) {
            joinCond.append("AND so.ORDER_DATE <= :toDate ");
            params.addValue("toDate", Timestamp.valueOf(toDate));
        }
        if ("ONLINE".equalsIgnoreCase(channel)) {
            joinCond.append("AND so.EMPLOYEE_ID IS NULL ");
        } else if ("POS".equalsIgnoreCase(channel)) {
            joinCond.append("AND so.EMPLOYEE_ID IS NOT NULL ");
        }

        String sql = "SELECT c.ID AS customer_id, c.CUSTOMER_CODE AS customer_code, c.FULL_NAME AS customer_name, " +
                "c.PHONE AS phone, COUNT(so.ID) AS completed_cnt, COALESCE(SUM(so.FINAL_AMOUNT), 0) AS total_spent " +
                "FROM CUSTOMERS c LEFT JOIN SALES_ORDERS so ON " + joinCond +
                "GROUP BY c.ID, c.CUSTOMER_CODE, c.FULL_NAME, c.PHONE " +
                "ORDER BY total_spent DESC, c.ID ASC LIMIT 50";

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> ComprehensiveReportResponse.CustomerSalesStatDto.builder()
                .customerId(rs.getLong("customer_id"))
                .customerCode(rs.getString("customer_code"))
                .customerName(rs.getString("customer_name"))
                .phone(rs.getString("phone"))
                .completedOrderCount(rs.getLong("completed_cnt"))
                .totalSpent(nvl(rs.getBigDecimal("total_spent")))
                .build());
    }

    @Override
    public List<ComprehensiveReportResponse.PurchaseSupplierStatDto> queryPurchaseStats(
            LocalDateTime fromDate, LocalDateTime toDate) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder joinCond = new StringBuilder("po.SUPPLIER_ID = s.ID AND po.STATUS = 'CONFIRMED' ");
        if (fromDate != null) {
            joinCond.append("AND po.PURCHASE_DATE >= :fromDate ");
            params.addValue("fromDate", Timestamp.valueOf(fromDate));
        }
        if (toDate != null) {
            joinCond.append("AND po.PURCHASE_DATE <= :toDate ");
            params.addValue("toDate", Timestamp.valueOf(toDate));
        }

        String sql = "SELECT s.ID AS supplier_id, s.SUPPLIER_CODE AS supplier_code, s.SUPPLIER_NAME AS supplier_name, " +
                "COUNT(DISTINCT po.ID) AS confirmed_cnt, COALESCE(SUM(pod.QUANTITY), 0) AS items_imported, " +
                "COALESCE(SUM(pod.SUBTOTAL), 0) AS total_amount " +
                "FROM SUPPLIERS s " +
                "LEFT JOIN PURCHASE_ORDERS po ON " + joinCond +
                "LEFT JOIN PURCHASE_ORDER_DETAILS pod ON pod.PURCHASE_ORDER_ID = po.ID " +
                "GROUP BY s.ID, s.SUPPLIER_CODE, s.SUPPLIER_NAME ORDER BY total_amount DESC, s.ID ASC";

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> ComprehensiveReportResponse.PurchaseSupplierStatDto.builder()
                .supplierId(rs.getLong("supplier_id"))
                .supplierCode(rs.getString("supplier_code"))
                .supplierName(rs.getString("supplier_name"))
                .confirmedCount(rs.getLong("confirmed_cnt"))
                .totalItemsImported(rs.getLong("items_imported"))
                .totalAmount(nvl(rs.getBigDecimal("total_amount")))
                .build());
    }

    private String classifyCancelReason(String note) {
        if (note == null || note.isBlank()) return "Lý do khác / Nội bộ";
        String low = note.toLowerCase();
        if (low.contains("đổi ý") || low.contains("doi y") || low.contains("không muốn mua") || low.contains("khách hủy")) {
            return "Khách đổi ý";
        }
        if (low.contains("địa chỉ") || low.contains("dia chi") || low.contains("sai sđt") || low.contains("sai thông tin")) {
            return "Sai thông tin địa chỉ / SĐT";
        }
        if (low.contains("liên lạc") || low.contains("lien lac") || low.contains("không nghe máy") || low.contains("thuê bao")) {
            return "Không liên lạc được khi giao hàng";
        }
        if (low.contains("size") || low.contains("mẫu") || low.contains("không ưng") || low.contains("từ chối nhận") || low.contains("bom")) {
            return "Khách từ chối nhận (Không ưng mẫu / size)";
        }
        return "Lý do khác / Nội bộ";
    }

    private String classifySizeGroup(String sizeVal) {
        if (sizeVal == null) return "KHAC";
        try {
            double num = Double.parseDouble(sizeVal.replaceAll("[^0-9.]", ""));
            if (num >= 35 && num <= 39.5) return "SIZE_NU";
            if (num >= 40 && num <= 46) return "SIZE_NAM";
        } catch (Exception ignored) {
        }
        return "KHAC";
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
