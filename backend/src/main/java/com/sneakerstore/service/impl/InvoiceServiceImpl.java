package com.sneakerstore.service.impl;

import com.sneakerstore.common.constant.AppConstants;
import com.sneakerstore.common.exception.BusinessException;
import com.sneakerstore.common.exception.ResourceNotFoundException;
import com.sneakerstore.common.response.PageResponse;
import com.sneakerstore.common.util.SecurityUtils;
import com.sneakerstore.dto.request.InvoiceAdjustRequest;
import com.sneakerstore.dto.request.InvoiceIssueRequest;
import com.sneakerstore.dto.request.ShippingWebhookRequest;
import com.sneakerstore.dto.response.InvoiceDto;
import com.sneakerstore.dto.response.InvoiceKpiDto;
import com.sneakerstore.entity.*;
import com.sneakerstore.exporter.InvoiceExcelExporter;
import com.sneakerstore.exporter.InvoicePdfGenerator;
import com.sneakerstore.repository.*;
import com.sneakerstore.service.InvoiceEmailSender;
import com.sneakerstore.service.InvoiceService;
import com.sneakerstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceDetailRepository detailRepository;
    private final InvoiceHistoryRepository historyRepository;
    private final InvoiceCancelRequestRepository cancelRequestRepository;
    private final InvoiceEmailHistoryRepository emailHistoryRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderDetailRepository salesOrderDetailRepository;
    private final PaymentRepository paymentRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final InvoicePdfGenerator pdfGenerator;
    private final InvoiceExcelExporter excelExporter;
    private final InvoiceEmailSender emailSender;
    private final ObjectProvider<OrderService> orderServiceProvider;

    @Value("${app.invoice.default-tax-rate:8}")
    private BigDecimal configuredDefaultTaxRate;

    @Value("${app.invoice.auto-email-on-issue:false}")
    private boolean autoEmailOnIssue;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final DateTimeFormatter CODE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Set<BigDecimal> ALLOWED_TAX_RATES = Set.of(
            BigDecimal.ZERO, BigDecimal.valueOf(5), BigDecimal.valueOf(8), BigDecimal.valueOf(10));

    // ==================== LIST, KPI & DETAIL ====================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InvoiceDto> searchInvoices(
            String keyword, String saleChannel, String invoiceType,
            String invoiceStatus, String emailStatus,
            LocalDate fromDate, LocalDate toDate, Pageable pageable) {

        LocalDateTime start = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime end = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
        String scopedUsername = resolveStaffScopeUsername();

        Page<Invoice> page = invoiceRepository.searchInvoices(
                trimOrNull(keyword),
                upperOrNull(saleChannel),
                upperOrNull(invoiceType),
                upperOrNull(invoiceStatus),
                upperOrNull(emailStatus),
                start, end, scopedUsername, pageable);

        return PageResponse.of(page.map(this::toSummaryDto));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceKpiDto getKpi(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime start = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime end = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
        String scopedUsername = resolveStaffScopeUsername();

        Object[] row = invoiceRepository.aggregateKpi(start, end, scopedUsername);
        if (row != null && row.length == 1 && row[0] instanceof Object[]) {
            row = (Object[]) row[0];
        }
        if (row == null || row.length < 7) {
            return InvoiceKpiDto.builder()
                    .totalInvoices(0)
                    .totalValidRevenue(BigDecimal.ZERO)
                    .totalValidTax(BigDecimal.ZERO)
                    .posInvoices(0)
                    .onlineInvoices(0)
                    .cancelledInvoices(0)
                    .pendingCancelCount(0)
                    .build();
        }
        return InvoiceKpiDto.builder()
                .totalInvoices(toLong(row[0]))
                .totalValidRevenue(toBigDecimal(row[1]))
                .totalValidTax(toBigDecimal(row[2]))
                .posInvoices(toLong(row[3]))
                .onlineInvoices(toLong(row[4]))
                .cancelledInvoices(toLong(row[5]))
                .pendingCancelCount(toLong(row[6]))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDto getById(Long id) {
        Invoice invoice = invoiceRepository.findWithRelationsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn với ID: " + id));
        enforceViewAccess(invoice);
        return toFullDto(invoice);
    }

    // ==================== AUTO & MANUAL ISSUANCE ====================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getVatConfig() {
        BigDecimal dbRate = resolveDbPosVatRate();
        String desc = systemSettingRepository.findById("POS_VAT_RATE")
                .map(SystemSetting::getDescription)
                .orElse("Thuế suất VAT (%) áp dụng tại quầy POS — cấu hình động trong DB");
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("posVatRate", dbRate);
        map.put("locked", true);
        map.put("source", "DATABASE");
        map.put("description", desc);
        return map;
    }

    private BigDecimal resolveDbPosVatRate() {
        return systemSettingRepository.findById("POS_VAT_RATE")
                .map(SystemSetting::getSettingValue)
                .map(v -> {
                    try {
                        return normalizeTaxRate(new BigDecimal(v.trim()));
                    } catch (Exception e) {
                        return configuredDefaultTaxRate != null ? configuredDefaultTaxRate : BigDecimal.valueOf(8);
                    }
                })
                .orElse(configuredDefaultTaxRate != null ? configuredDefaultTaxRate : BigDecimal.valueOf(8));
    }

    @Override
    @Transactional
    public InvoiceDto autoIssueForCompletedOrder(Long orderId, InvoiceIssueRequest optionalConfig) {
        SalesOrder order = salesOrderRepository.findWithHeaderById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        // Quy tắc 2.1 & 2.2: Chỉ phát sinh hóa đơn khi đơn hàng ĐÃ HOÀN THÀNH (COMPLETED)
        if (!AppConstants.ORDER_COMPLETED.equalsIgnoreCase(order.getStatus())) {
            return null;
        }

        // Idempotency check: đã tồn tại hóa đơn gốc cho đơn hàng này -> trả về ngay, không tạo trùng
        Optional<Invoice> existing = invoiceRepository.findOriginalByOrderId(orderId);
        if (existing.isPresent()) {
            return toSummaryDto(existing.get());
        }

        try {
            Invoice created = createInvoiceFromOrder(order, optionalConfig, "ORIGINAL", null, null);
            return toFullDto(created);
        } catch (DataIntegrityViolationException ex) {
            // Phòng trường hợp race condition đồng thời (unique partial index uq_invoices_original_order)
            log.warn("[InvoiceService] Concurrent invoice creation prevented for order {}: {}", order.getOrderCode(), ex.getMessage());
            return invoiceRepository.findOriginalByOrderId(orderId)
                    .map(this::toSummaryDto)
                    .orElseThrow(() -> ex);
        }
    }

    @Override
    @Transactional
    public InvoiceDto issueManualInvoice(InvoiceIssueRequest request) {
        if (request == null || request.getOrderId() == null) {
            throw new BusinessException("Vui lòng chọn đơn hàng nguồn để phát hành hóa đơn");
        }
        SalesOrder order = salesOrderRepository.findWithHeaderById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + request.getOrderId()));

        if (!AppConstants.ORDER_COMPLETED.equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException("Đơn hàng " + order.getOrderCode() + " đang ở trạng thái "
                    + order.getStatus() + ", chỉ được phát hành hóa đơn khi đơn đã HOÀN THÀNH (COMPLETED)");
        }

        String requestedType = normalizeInvoiceType(request.getInvoiceType());
        validateVatFieldsIfNeeded(requestedType, request.getCompanyName(), request.getTaxCode(), request.getCustomerEmail());

        Optional<Invoice> existingOpt = invoiceRepository.findOriginalByOrderId(order.getId());
        if (existingOpt.isPresent()) {
            Invoice existing = existingOpt.get();
            if ("VAT".equals(requestedType)
                    && "RETAIL".equalsIgnoreCase(existing.getInvoiceType())
                    && "ISSUED".equalsIgnoreCase(existing.getInvoiceStatus())) {
                // Chuyển đổi từ Hóa đơn bán lẻ sang Hóa đơn VAT theo yêu cầu khách hàng bằng hóa đơn thay thế (giữ nguyên lịch sử hóa đơn gốc)
                String actor = SecurityUtils.getCurrentUsername();
                existing.setInvoiceStatus("ADJUSTED");
                existing.setAdjustmentReason("Phát hành hóa đơn VAT thay thế theo yêu cầu khách hàng");
                existing.setUpdatedBy(actor);
                invoiceRepository.save(existing);

                historyRepository.save(InvoiceHistory.builder()
                        .invoice(existing)
                        .action("ADJUSTED")
                        .fromStatus("ISSUED")
                        .toStatus("ADJUSTED")
                        .actor(actor)
                        .reason("Khách hàng yêu cầu xuất hóa đơn VAT thay thế hóa đơn bán lẻ")
                        .build());

                Invoice vatInvoice = createInvoiceFromOrder(order, request, "REPLACEMENT", existing,
                        "Xuất hóa đơn VAT thay thế cho " + existing.getInvoiceCode());
                return toFullDto(vatInvoice);
            }
            throw new BusinessException("Đơn hàng " + order.getOrderCode()
                    + " đã phát hành hóa đơn " + existing.getInvoiceCode() + ", không cho phép phát hành trùng lặp!");
        }

        Invoice created = createInvoiceFromOrder(order, request, "ORIGINAL", null, null);
        return toFullDto(created);
    }

    private Invoice createInvoiceFromOrder(
            SalesOrder order,
            InvoiceIssueRequest cfg,
            String recordKind,
            Invoice originalInvoice,
            String adjustmentReason) {

        String actor = SecurityUtils.getCurrentUsername();
        String channel = order.getEmployee() != null ? "POS" : "ONLINE";
        String invoiceType = cfg != null ? normalizeInvoiceType(cfg.getInvoiceType()) : "RETAIL";

        // Customer snapshot (ưu tiên dữ liệu request -> hồ sơ Customer -> ghi chú Online)
        Customer cust = order.getCustomer();
        String noteReceiver = extractFromOrderNote(order.getNote(), "Người nhận");
        String notePhone = extractFromOrderNote(order.getNote(), "SĐT");
        String noteAddress = extractFromOrderNote(order.getNote(), "Đ/C nhận");

        String custName = firstNonBlank(
                cfg != null ? cfg.getCustomerName() : null,
                "ONLINE".equals(channel) ? noteReceiver : null,
                cust != null ? cust.getFullName() : null,
                "Khách lẻ tại quầy");
        String custPhone = firstNonBlank(
                cfg != null ? cfg.getCustomerPhone() : null,
                "ONLINE".equals(channel) ? notePhone : null,
                cust != null ? cust.getPhone() : null,
                null);
        String custEmail = firstNonBlank(
                cfg != null ? cfg.getCustomerEmail() : null,
                cust != null ? cust.getEmail() : null,
                null);
        String custAddress = firstNonBlank(
                cfg != null ? cfg.getCustomerAddress() : null,
                "ONLINE".equals(channel) ? noteAddress : null,
                cust != null ? cust.getAddress() : null,
                null);
        String companyName = cfg != null ? trimOrNull(cfg.getCompanyName()) : null;
        String taxCode = cfg != null ? trimOrNull(cfg.getTaxCode()) : null;

        if (custEmail != null && !EMAIL_PATTERN.matcher(custEmail).matches()) {
            throw new BusinessException("Email nhận hóa đơn không đúng định dạng: " + custEmail);
        }

        BigDecimal dbPosVatRate = resolveDbPosVatRate();
        // Với kênh POS, luôn ép dùng thuế suất VAT cấu hình động từ DB (không cho phép sửa từ giao diện, kể cả Admin)
        BigDecimal defaultRate = "POS".equals(channel)
                ? dbPosVatRate
                : normalizeTaxRate(cfg != null && cfg.getDefaultTaxRate() != null
                        ? cfg.getDefaultTaxRate()
                        : dbPosVatRate);
        boolean taxIncluded = cfg == null || cfg.getTaxIncluded() == null || cfg.getTaxIncluded();

        Map<Long, InvoiceIssueRequest.ItemTaxOverride> overrideMap = new HashMap<>();
        if (cfg != null && cfg.getItemTaxOverrides() != null) {
            for (InvoiceIssueRequest.ItemTaxOverride ov : cfg.getItemTaxOverrides()) {
                if (ov.getProductVariantId() != null) {
                    overrideMap.put(ov.getProductVariantId(), ov);
                }
            }
        }

        List<SalesOrderDetail> orderDetails = salesOrderDetailRepository.findBySalesOrderIdOrderByIdAsc(order.getId());
        BigDecimal orderDiscount = BigDecimal.ZERO;
        if (cfg != null && cfg.getDiscountAmount() != null && cfg.getDiscountAmount().compareTo(BigDecimal.ZERO) >= 0) {
            orderDiscount = cfg.getDiscountAmount();
        } else if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            orderDiscount = order.getDiscountAmount();
        } else if (order.getTotalAmount() != null && order.getFinalAmount() != null
                && order.getTotalAmount().compareTo(order.getFinalAmount()) > 0) {
            orderDiscount = order.getTotalAmount().subtract(order.getFinalAmount());
        }

        BigDecimal shippingFee = (cfg != null && cfg.getShippingFee() != null && cfg.getShippingFee().compareTo(BigDecimal.ZERO) >= 0)
                ? cfg.getShippingFee()
                : (order.getShippingFee() != null && order.getShippingFee().compareTo(BigDecimal.ZERO) > 0
                        ? order.getShippingFee()
                        : BigDecimal.ZERO);

        List<InvoiceDetail> invoiceDetails = new ArrayList<>();
        BigDecimal subtotalBeforeTax = BigDecimal.ZERO;
        BigDecimal totalLineDiscounts = BigDecimal.ZERO;
        BigDecimal totalTaxAmount = BigDecimal.ZERO;
        Set<BigDecimal> usedRates = new HashSet<>();

        for (SalesOrderDetail sod : orderDetails) {
            ProductVariant pv = sod.getProductVariant();
            InvoiceIssueRequest.ItemTaxOverride ov = pv != null ? overrideMap.get(pv.getId()) : null;
            BigDecimal lineRate = ov != null && ov.getTaxRate() != null ? normalizeTaxRate(ov.getTaxRate()) : defaultRate;
            BigDecimal lineDiscount = ov != null && ov.getDiscountAmount() != null
                    ? ov.getDiscountAmount().max(BigDecimal.ZERO)
                    : BigDecimal.ZERO;

            usedRates.add(lineRate);
            BigDecimal grossLine = sod.getUnitPrice().multiply(BigDecimal.valueOf(sod.getQuantity()));
            BigDecimal afterLineDiscount = grossLine.subtract(lineDiscount).max(BigDecimal.ZERO);

            BigDecimal linePreTax;
            BigDecimal lineTax;
            BigDecimal lineTotal;
            if (taxIncluded) {
                BigDecimal divisor = BigDecimal.ONE.add(lineRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
                linePreTax = afterLineDiscount.divide(divisor, 0, RoundingMode.HALF_UP);
                lineTax = afterLineDiscount.subtract(linePreTax);
                lineTotal = afterLineDiscount;
            } else {
                linePreTax = afterLineDiscount.setScale(0, RoundingMode.HALF_UP);
                lineTax = linePreTax.multiply(lineRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                lineTotal = linePreTax.add(lineTax);
            }

            subtotalBeforeTax = subtotalBeforeTax.add(linePreTax).add(lineDiscount);
            totalLineDiscounts = totalLineDiscounts.add(lineDiscount);
            totalTaxAmount = totalTaxAmount.add(lineTax);

            String variantLabel = "";
            if (pv != null) {
                String sz = pv.getSize() != null ? "Size " + pv.getSize().getSizeValue() : "";
                String col = pv.getColor() != null ? pv.getColor().getColorName() : "";
                variantLabel = (!sz.isEmpty() && !col.isEmpty()) ? (sz + " / " + col) : (sz + col);
            }

            invoiceDetails.add(InvoiceDetail.builder()
                    .productId(pv != null && pv.getProduct() != null ? pv.getProduct().getId() : null)
                    .productVariantId(pv != null ? pv.getId() : null)
                    .productCode(pv != null && pv.getProduct() != null ? pv.getProduct().getProductCode() : null)
                    .productName(pv != null && pv.getProduct() != null ? pv.getProduct().getProductName() : "Sneaker")
                    .sku(pv != null ? pv.getSku() : null)
                    .variantLabel(variantLabel)
                    .quantity(sod.getQuantity())
                    .unitPrice(sod.getUnitPrice())
                    .discountAmount(lineDiscount)
                    .taxRate(lineRate)
                    .taxAmount(lineTax)
                    .lineTotal(lineTotal)
                    .createdBy(actor)
                    .updatedBy(actor)
                    .build());
        }

        // Không trừ giảm giá 2 lần: nếu chưa phân bổ lineDiscount thì lấy orderDiscount
        BigDecimal effectiveDiscount = totalLineDiscounts.compareTo(BigDecimal.ZERO) > 0
                ? totalLineDiscounts
                : orderDiscount;

        // Nếu có order-level discount trong mode taxIncluded=false, tính lại taxable & tax nhất quán
        BigDecimal taxableAmount = subtotalBeforeTax.subtract(effectiveDiscount).max(BigDecimal.ZERO);
        BigDecimal summaryTaxRate = usedRates.size() == 1
                ? usedRates.iterator().next()
                : defaultRate;

        // Đảm bảo công thức bất biến Section 5.6: Final Total = Subtotal - Discount + Shipping Fee + Tax Amount
        BigDecimal finalTotal = subtotalBeforeTax
                .subtract(effectiveDiscount)
                .add(shippingFee)
                .add(totalTaxAmount)
                .max(BigDecimal.ZERO)
                .setScale(0, RoundingMode.HALF_UP);

        String paymentStatus = resolveOrderPaymentStatus(order);
        String invoiceCode = generateInvoiceCode(invoiceType, recordKind);

        Invoice invoice = Invoice.builder()
                .invoiceCode(invoiceCode)
                .salesOrder(order)
                .originalInvoice(originalInvoice)
                .saleChannel(channel)
                .invoiceType(invoiceType)
                .recordKind(recordKind)
                .customer(cust)
                .customerName(custName)
                .customerPhone(custPhone)
                .customerEmail(custEmail)
                .customerAddress(custAddress)
                .companyName(companyName)
                .taxCode(taxCode)
                .subtotal(subtotalBeforeTax.setScale(0, RoundingMode.HALF_UP))
                .discountAmount(effectiveDiscount.setScale(0, RoundingMode.HALF_UP))
                .shippingFee(shippingFee.setScale(0, RoundingMode.HALF_UP))
                .taxableAmount(taxableAmount.setScale(0, RoundingMode.HALF_UP))
                .taxRate(summaryTaxRate)
                .taxAmount(totalTaxAmount.setScale(0, RoundingMode.HALF_UP))
                .totalAmount(finalTotal)
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod() : "CASH")
                .paymentStatus(paymentStatus)
                .invoiceStatus("ISSUED")
                .emailStatus("NOT_SENT")
                .issuedAt(LocalDateTime.now())
                .adjustmentReason(adjustmentReason)
                .note(firstNonBlank(cfg != null ? cfg.getNote() : null, order.getNote(), null))
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        for (InvoiceDetail d : invoiceDetails) {
            d.setInvoice(invoice);
        }
        invoice.setDetails(invoiceDetails);

        Invoice saved = invoiceRepository.save(invoice);

        historyRepository.save(InvoiceHistory.builder()
                .invoice(saved)
                .action("VAT".equals(invoiceType) ? "VAT_ISSUED" : "ISSUED")
                .fromStatus(null)
                .toStatus("ISSUED")
                .actor(actor)
                .reason(adjustmentReason != null ? adjustmentReason : "Phát hành hóa đơn từ đơn hàng hoàn thành " + order.getOrderCode())
                .detail(String.format("Kênh: %s | Loại: %s | Trước thuế: %s | VAT: %s | Tổng: %s",
                        channel, invoiceType, saved.getSubtotal(), saved.getTaxAmount(), saved.getTotalAmount()))
                .build());

        boolean shouldAutoSend = (cfg != null && Boolean.TRUE.equals(cfg.getAutoSendEmail())) || autoEmailOnIssue;
        if (shouldAutoSend && custEmail != null && EMAIL_PATTERN.matcher(custEmail).matches()) {
            triggerEmailDispatch(saved, custEmail, actor);
        }

        return saved;
    }

    // ==================== CANCEL & CANCEL REQUEST WORKFLOW ====================

    @Override
    @Transactional
    public InvoiceDto cancelInvoiceDirect(Long id, String reason) {
        if (!SecurityUtils.isAdminOrManager()) {
            throw new AccessDeniedException("Nhân viên bán hàng không có quyền trực tiếp hủy hóa đơn. Vui lòng gửi yêu cầu duyệt hủy.");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Bắt buộc nhập lý do hủy hóa đơn");
        }

        Invoice invoice = invoiceRepository.findWithRelationsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn với ID: " + id));

        if ("CANCELLED".equalsIgnoreCase(invoice.getInvoiceStatus())) {
            throw new BusinessException("Hóa đơn " + invoice.getInvoiceCode() + " đã ở trạng thái ĐÃ HỦY");
        }

        String actor = SecurityUtils.getCurrentUsername();
        String prevStatus = invoice.getInvoiceStatus();

        invoice.setInvoiceStatus("CANCELLED");
        invoice.setCancelledAt(LocalDateTime.now());
        invoice.setCancelReason(reason.trim());
        invoice.setCancelledBy(actor);
        invoice.setUpdatedBy(actor);
        invoiceRepository.save(invoice);

        // Nếu có yêu cầu hủy đang chờ duyệt -> tự động đánh dấu APPROVED
        cancelRequestRepository.findFirstByInvoiceIdAndStatusOrderByRequestedAtDesc(id, "PENDING")
                .ifPresent(req -> {
                    req.setStatus("APPROVED");
                    req.setReviewedBy(actor);
                    req.setReviewNote("Duyệt hủy trực tiếp: " + reason.trim());
                    req.setReviewedAt(LocalDateTime.now());
                    cancelRequestRepository.save(req);
                });

        historyRepository.save(InvoiceHistory.builder()
                .invoice(invoice)
                .action("CANCELLED")
                .fromStatus(prevStatus)
                .toStatus("CANCELLED")
                .actor(actor)
                .reason(reason.trim())
                .detail("Hủy hóa đơn trực tiếp bởi " + actor + " (dữ liệu hóa đơn gốc vẫn được lưu trữ bất biến)")
                .build());

        return toFullDto(invoice);
    }

    @Override
    @Transactional
    public InvoiceDto requestCancelInvoice(Long id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Bắt buộc nhập lý do yêu cầu hủy hóa đơn");
        }
        Invoice invoice = invoiceRepository.findWithRelationsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn với ID: " + id));
        enforceViewAccess(invoice);

        if ("CANCELLED".equalsIgnoreCase(invoice.getInvoiceStatus())) {
            throw new BusinessException("Hóa đơn " + invoice.getInvoiceCode() + " đã bị hủy trước đó");
        }
        if ("PENDING_CANCEL".equalsIgnoreCase(invoice.getInvoiceStatus())
                || cancelRequestRepository.existsByInvoiceIdAndStatus(id, "PENDING")) {
            throw new BusinessException("Hóa đơn " + invoice.getInvoiceCode() + " đang có yêu cầu hủy chờ duyệt");
        }

        String actor = SecurityUtils.getCurrentUsername();
        String prevStatus = invoice.getInvoiceStatus();

        invoice.setInvoiceStatus("PENDING_CANCEL");
        invoice.setUpdatedBy(actor);
        invoiceRepository.save(invoice);

        cancelRequestRepository.save(InvoiceCancelRequest.builder()
                .invoice(invoice)
                .requestedBy(actor)
                .reason(reason.trim())
                .status("PENDING")
                .requestedAt(LocalDateTime.now())
                .build());

        historyRepository.save(InvoiceHistory.builder()
                .invoice(invoice)
                .action("CANCEL_REQUESTED")
                .fromStatus(prevStatus)
                .toStatus("PENDING_CANCEL")
                .actor(actor)
                .reason(reason.trim())
                .detail("Nhân viên " + actor + " gửi yêu cầu duyệt hủy hóa đơn")
                .build());

        return toFullDto(invoice);
    }

    @Override
    @Transactional
    public InvoiceDto approveCancelRequest(Long requestId, String reviewNote) {
        if (!SecurityUtils.isAdminOrManager()) {
            throw new AccessDeniedException("Chỉ Admin hoặc Quản lý mới được phép duyệt hủy hóa đơn");
        }
        InvoiceCancelRequest req = cancelRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hủy hóa đơn ID: " + requestId));
        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new BusinessException("Yêu cầu hủy này đã được xử lý (" + req.getStatus() + ")");
        }

        String actor = SecurityUtils.getCurrentUsername();
        Invoice invoice = req.getInvoice();
        String prevStatus = invoice.getInvoiceStatus();

        req.setStatus("APPROVED");
        req.setReviewedBy(actor);
        req.setReviewNote(trimOrNull(reviewNote));
        req.setReviewedAt(LocalDateTime.now());
        cancelRequestRepository.save(req);

        invoice.setInvoiceStatus("CANCELLED");
        invoice.setCancelledAt(LocalDateTime.now());
        invoice.setCancelReason(req.getReason());
        invoice.setCancelledBy(actor);
        invoice.setUpdatedBy(actor);
        invoiceRepository.save(invoice);

        historyRepository.save(InvoiceHistory.builder()
                .invoice(invoice)
                .action("CANCEL_APPROVED")
                .fromStatus(prevStatus)
                .toStatus("CANCELLED")
                .actor(actor)
                .reason(req.getReason())
                .detail("Duyệt yêu cầu hủy của " + req.getRequestedBy()
                        + (reviewNote != null && !reviewNote.isBlank() ? " | Ghi chú duyệt: " + reviewNote.trim() : ""))
                .build());

        return toFullDto(invoice);
    }

    @Override
    @Transactional
    public InvoiceDto rejectCancelRequest(Long requestId, String reviewNote) {
        if (!SecurityUtils.isAdminOrManager()) {
            throw new AccessDeniedException("Chỉ Admin hoặc Quản lý mới được phép từ chối yêu cầu hủy hóa đơn");
        }
        InvoiceCancelRequest req = cancelRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hủy hóa đơn ID: " + requestId));
        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new BusinessException("Yêu cầu hủy này đã được xử lý (" + req.getStatus() + ")");
        }

        String actor = SecurityUtils.getCurrentUsername();
        Invoice invoice = req.getInvoice();

        req.setStatus("REJECTED");
        req.setReviewedBy(actor);
        req.setReviewNote(firstNonBlank(reviewNote, "Từ chối yêu cầu hủy", "Từ chối"));
        req.setReviewedAt(LocalDateTime.now());
        cancelRequestRepository.save(req);

        invoice.setInvoiceStatus("ISSUED");
        invoice.setUpdatedBy(actor);
        invoiceRepository.save(invoice);

        historyRepository.save(InvoiceHistory.builder()
                .invoice(invoice)
                .action("CANCEL_REJECTED")
                .fromStatus("PENDING_CANCEL")
                .toStatus("ISSUED")
                .actor(actor)
                .reason(req.getReviewNote())
                .detail("Từ chối yêu cầu hủy của " + req.getRequestedBy() + ", khôi phục trạng thái Đã phát hành")
                .build());

        return toFullDto(invoice);
    }

    // ==================== ADJUSTMENT / REPLACEMENT INVOICE ====================

    @Override
    @Transactional
    public InvoiceDto adjustInvoice(Long originalInvoiceId, InvoiceAdjustRequest request) {
        if (!SecurityUtils.isAdminOrManager()) {
            throw new AccessDeniedException("Chỉ Admin hoặc Quản lý mới có quyền lập hóa đơn điều chỉnh / thay thế");
        }
        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessException("Bắt buộc nhập lý do điều chỉnh / thay thế hóa đơn");
        }

        Invoice original = invoiceRepository.findWithRelationsById(originalInvoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn gốc ID: " + originalInvoiceId));

        if ("CANCELLED".equalsIgnoreCase(original.getInvoiceStatus())) {
            throw new BusinessException("Không thể điều chỉnh hóa đơn đã bị hủy");
        }

        String recordKind = "REPLACEMENT".equalsIgnoreCase(request.getRecordKind()) ? "REPLACEMENT" : "ADJUSTMENT";
        String invoiceType = request.getInvoiceType() != null && !request.getInvoiceType().isBlank()
                ? normalizeInvoiceType(request.getInvoiceType())
                : original.getInvoiceType();

        String companyName = firstNonBlank(request.getCompanyName(), original.getCompanyName(), null);
        String taxCode = firstNonBlank(request.getTaxCode(), original.getTaxCode(), null);
        String email = firstNonBlank(request.getCustomerEmail(), original.getCustomerEmail(), null);
        validateVatFieldsIfNeeded(invoiceType, companyName, taxCode, email);

        String actor = SecurityUtils.getCurrentUsername();
        List<InvoiceDetail> origDetails = detailRepository.findByInvoiceIdOrderByIdAsc(originalInvoiceId);

        boolean taxIncluded = Boolean.TRUE.equals(request.getTaxIncluded());
        BigDecimal shippingFee = request.getShippingFee() != null ? request.getShippingFee().max(BigDecimal.ZERO) : original.getShippingFee();

        List<InvoiceDetail> newDetails = new ArrayList<>();
        BigDecimal subtotalBeforeTax = BigDecimal.ZERO;
        BigDecimal totalLineDiscounts = BigDecimal.ZERO;
        BigDecimal totalTaxAmount = BigDecimal.ZERO;
        Set<BigDecimal> usedRates = new HashSet<>();

        List<InvoiceAdjustRequest.AdjustItem> inputItems = request.getItems();
        if (inputItems == null || inputItems.isEmpty()) {
            inputItems = origDetails.stream().map(d -> new InvoiceAdjustRequest.AdjustItem(
                    d.getProductId(), d.getProductVariantId(), d.getProductCode(), d.getProductName(),
                    d.getSku(), d.getVariantLabel(), d.getQuantity(), d.getUnitPrice(),
                    d.getDiscountAmount(), d.getTaxRate()
            )).collect(Collectors.toList());
        }

        for (InvoiceAdjustRequest.AdjustItem item : inputItems) {
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            if (qty <= 0) continue;
            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice().max(BigDecimal.ZERO) : BigDecimal.ZERO;
            BigDecimal lineDiscount = item.getDiscountAmount() != null ? item.getDiscountAmount().max(BigDecimal.ZERO) : BigDecimal.ZERO;
            BigDecimal lineRate = normalizeTaxRate(item.getTaxRate() != null ? item.getTaxRate() : original.getTaxRate());
            usedRates.add(lineRate);

            BigDecimal grossLine = unitPrice.multiply(BigDecimal.valueOf(qty));
            BigDecimal afterLineDiscount = grossLine.subtract(lineDiscount).max(BigDecimal.ZERO);

            BigDecimal linePreTax;
            BigDecimal lineTax;
            BigDecimal lineTotal;
            if (taxIncluded) {
                BigDecimal divisor = BigDecimal.ONE.add(lineRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
                linePreTax = afterLineDiscount.divide(divisor, 0, RoundingMode.HALF_UP);
                lineTax = afterLineDiscount.subtract(linePreTax);
                lineTotal = afterLineDiscount;
            } else {
                linePreTax = afterLineDiscount.setScale(0, RoundingMode.HALF_UP);
                lineTax = linePreTax.multiply(lineRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                lineTotal = linePreTax.add(lineTax);
            }

            subtotalBeforeTax = subtotalBeforeTax.add(linePreTax).add(lineDiscount);
            totalLineDiscounts = totalLineDiscounts.add(lineDiscount);
            totalTaxAmount = totalTaxAmount.add(lineTax);

            newDetails.add(InvoiceDetail.builder()
                    .productId(item.getProductId())
                    .productVariantId(item.getProductVariantId())
                    .productCode(item.getProductCode())
                    .productName(item.getProductName() != null ? item.getProductName() : "Sản phẩm điều chỉnh")
                    .sku(item.getSku())
                    .variantLabel(item.getVariantLabel())
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .discountAmount(lineDiscount)
                    .taxRate(lineRate)
                    .taxAmount(lineTax)
                    .lineTotal(lineTotal)
                    .createdBy(actor)
                    .updatedBy(actor)
                    .build());
        }

        if (newDetails.isEmpty()) {
            throw new BusinessException("Hóa đơn điều chỉnh phải có ít nhất 1 dòng sản phẩm hợp lệ");
        }

        BigDecimal effectiveDiscount = totalLineDiscounts.compareTo(BigDecimal.ZERO) > 0
                ? totalLineDiscounts
                : (request.getDiscountAmount() != null ? request.getDiscountAmount().max(BigDecimal.ZERO) : BigDecimal.ZERO);

        BigDecimal taxableAmount = subtotalBeforeTax.subtract(effectiveDiscount).max(BigDecimal.ZERO);
        BigDecimal summaryRate = usedRates.size() == 1 ? usedRates.iterator().next() : original.getTaxRate();
        BigDecimal finalTotal = subtotalBeforeTax
                .subtract(effectiveDiscount)
                .add(shippingFee)
                .add(totalTaxAmount)
                .max(BigDecimal.ZERO)
                .setScale(0, RoundingMode.HALF_UP);

        // Không sửa dữ liệu tài chính của hóa đơn gốc; chỉ đánh dấu trạng thái ADJUSTED
        String origPrevStatus = original.getInvoiceStatus();
        original.setInvoiceStatus("ADJUSTED");
        original.setAdjustmentReason(request.getReason().trim());
        original.setUpdatedBy(actor);
        invoiceRepository.save(original);

        Invoice adjusted = Invoice.builder()
                .invoiceCode(generateInvoiceCode(invoiceType, recordKind))
                .salesOrder(original.getSalesOrder())
                .originalInvoice(original)
                .saleChannel(original.getSaleChannel())
                .invoiceType(invoiceType)
                .recordKind(recordKind)
                .customer(original.getCustomer())
                .customerName(firstNonBlank(request.getCustomerName(), original.getCustomerName(), "Khách lẻ"))
                .customerPhone(firstNonBlank(request.getCustomerPhone(), original.getCustomerPhone(), null))
                .customerEmail(email)
                .customerAddress(firstNonBlank(request.getCustomerAddress(), original.getCustomerAddress(), null))
                .companyName(companyName)
                .taxCode(taxCode)
                .subtotal(subtotalBeforeTax.setScale(0, RoundingMode.HALF_UP))
                .discountAmount(effectiveDiscount.setScale(0, RoundingMode.HALF_UP))
                .shippingFee(shippingFee.setScale(0, RoundingMode.HALF_UP))
                .taxableAmount(taxableAmount.setScale(0, RoundingMode.HALF_UP))
                .taxRate(summaryRate)
                .taxAmount(totalTaxAmount.setScale(0, RoundingMode.HALF_UP))
                .totalAmount(finalTotal)
                .paymentMethod(original.getPaymentMethod())
                .paymentStatus(original.getPaymentStatus())
                .invoiceStatus("ISSUED")
                .emailStatus("NOT_SENT")
                .issuedAt(LocalDateTime.now())
                .adjustmentReason(request.getReason().trim())
                .note(firstNonBlank(request.getNote(), "Liên kết hóa đơn gốc: " + original.getInvoiceCode(), null))
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        for (InvoiceDetail d : newDetails) {
            d.setInvoice(adjusted);
        }
        adjusted.setDetails(newDetails);
        Invoice savedAdjusted = invoiceRepository.save(adjusted);

        historyRepository.save(InvoiceHistory.builder()
                .invoice(original)
                .action("ADJUSTED")
                .fromStatus(origPrevStatus)
                .toStatus("ADJUSTED")
                .actor(actor)
                .reason(request.getReason().trim())
                .detail("Đã lập hóa đơn " + recordKind + " mới: " + savedAdjusted.getInvoiceCode())
                .build());

        historyRepository.save(InvoiceHistory.builder()
                .invoice(savedAdjusted)
                .action("ISSUED")
                .fromStatus(null)
                .toStatus("ISSUED")
                .actor(actor)
                .reason(request.getReason().trim())
                .detail("Hóa đơn " + recordKind + " cho hóa đơn gốc " + original.getInvoiceCode())
                .build());

        return toFullDto(savedAdjusted);
    }

    // ==================== EMAIL DISPATCH ====================

    @Override
    @Transactional
    public InvoiceDto sendInvoiceEmail(Long id, String recipientEmail) {
        Invoice invoice = invoiceRepository.findWithRelationsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn với ID: " + id));
        enforceViewAccess(invoice);

        if ("SENDING".equalsIgnoreCase(invoice.getEmailStatus())) {
            throw new BusinessException("Hóa đơn " + invoice.getInvoiceCode() + " đang trong quá trình gửi Email, vui lòng chờ giây lát để tránh gửi trùng!");
        }

        String targetEmail = firstNonBlank(recipientEmail, invoice.getCustomerEmail(), null);
        if (targetEmail == null || targetEmail.isBlank()) {
            throw new BusinessException("Khách hàng chưa có Email. Vui lòng nhập địa chỉ Email nhận hóa đơn.");
        }
        if (!EMAIL_PATTERN.matcher(targetEmail.trim()).matches()) {
            throw new BusinessException("Địa chỉ Email không đúng định dạng: " + targetEmail);
        }

        String cleanEmail = targetEmail.trim();
        if (invoice.getCustomerEmail() == null || invoice.getCustomerEmail().isBlank()) {
            invoice.setCustomerEmail(cleanEmail);
        }

        String actor = SecurityUtils.getCurrentUsername();
        triggerEmailDispatch(invoice, cleanEmail, actor);
        return toFullDto(invoice);
    }

    private void triggerEmailDispatch(Invoice invoice, String recipientEmail, String actor) {
        invoice.setEmailStatus("SENDING");
        invoice.setUpdatedBy(actor);
        invoiceRepository.save(invoice);

        InvoiceEmailHistory emailHist = emailHistoryRepository.save(InvoiceEmailHistory.builder()
                .invoice(invoice)
                .recipientEmail(recipientEmail)
                .subject("Hóa đơn mua hàng – ADHA Sneaker")
                .sendStatus("SENDING")
                .sentBy(actor)
                .retryCount(0)
                .build());

        Long invoiceId = invoice.getId();
        Long emailHistId = emailHist.getId();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailSender.sendInvoiceEmailAsync(invoiceId, emailHistId, actor);
                }
            });
        } else {
            emailSender.sendInvoiceEmailAsync(invoiceId, emailHistId, actor);
        }
    }

    // ==================== PDF & EXCEL ====================

    @Override
    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Long id) {
        Invoice invoice = invoiceRepository.findWithRelationsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn với ID: " + id));
        enforceViewAccess(invoice);
        List<InvoiceDetail> details = detailRepository.findByInvoiceIdOrderByIdAsc(id);
        return pdfGenerator.generate(invoice, details);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportInvoicesExcel(
            String keyword, String saleChannel, String invoiceType,
            String invoiceStatus, String emailStatus,
            LocalDate fromDate, LocalDate toDate) {
        if (!SecurityUtils.isAdminOrManager()) {
            throw new AccessDeniedException("Chỉ Admin hoặc Quản lý mới được phép xuất báo cáo bảng kê hóa đơn Excel");
        }
        LocalDateTime start = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime end = toDate != null ? toDate.atTime(LocalTime.MAX) : null;

        Page<Invoice> page = invoiceRepository.searchInvoices(
                trimOrNull(keyword),
                upperOrNull(saleChannel),
                upperOrNull(invoiceType),
                upperOrNull(invoiceStatus),
                upperOrNull(emailStatus),
                start, end, null,
                PageRequest.of(0, 2000, Sort.by(Sort.Direction.DESC, "issuedAt")));
        return excelExporter.export(page.getContent());
    }

    // ==================== SHIPPING WEBHOOK (IDEMPOTENT) ====================

    @Override
    @Transactional
    public Map<String, Object> processShippingWebhook(ShippingWebhookRequest request) {
        if (request == null || request.getOrderCode() == null || request.getOrderCode().isBlank()) {
            throw new BusinessException("Thiếu mã đơn hàng (orderCode) trong webhook vận chuyển");
        }
        SalesOrder order = salesOrderRepository.findByOrderCodeIgnoreCase(request.getOrderCode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + request.getOrderCode()));

        String deliveryStatus = request.getDeliveryStatus() != null
                ? request.getDeliveryStatus().trim().toUpperCase()
                : "";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderCode", order.getOrderCode());
        result.put("deliveryStatus", deliveryStatus);

        if ("DELIVERED".equals(deliveryStatus) || "COMPLETED".equals(deliveryStatus)) {
            if (AppConstants.ORDER_CANCELLED.equalsIgnoreCase(order.getStatus())) {
                result.put("invoiceCreated", false);
                result.put("message", "Đơn hàng đã bị hủy, không phát sinh hóa đơn");
                return result;
            }
            boolean existedBefore = invoiceRepository.findOriginalByOrderId(order.getId()).isPresent();
            if (!AppConstants.ORDER_COMPLETED.equalsIgnoreCase(order.getStatus())) {
                String webhookNote = "Đơn vị vận chuyển"
                        + (request.getCarrierName() != null ? " (" + request.getCarrierName() + ")" : "")
                        + " xác nhận giao hàng thành công"
                        + (request.getTrackingCode() != null ? " - Mã vận đơn: " + request.getTrackingCode() : "");
                orderServiceProvider.getObject().updateStatus(order.getId(), AppConstants.ORDER_COMPLETED, webhookNote);
            }
            // Idempotent ensure invoice exists (if callback fired multiple times, returns exact same invoice)
            InvoiceDto inv = invoiceRepository.findOriginalByOrderId(order.getId())
                    .map(this::toSummaryDto)
                    .orElseGet(() -> autoIssueForCompletedOrder(order.getId(), null));

            result.put("invoiceCreated", !existedBefore && inv != null);
            result.put("idempotentHit", existedBefore);
            result.put("invoiceCode", inv != null ? inv.getInvoiceCode() : null);
            result.put("invoiceId", inv != null ? inv.getId() : null);
            result.put("message", existedBefore
                    ? "Callback trùng lặp (Idempotent) - đã tồn tại hóa đơn " + (inv != null ? inv.getInvoiceCode() : "")
                    : "Giao hàng thành công - đã tự động phát sinh hóa đơn " + (inv != null ? inv.getInvoiceCode() : ""));
            return result;
        }

        if (List.of("FAILED", "RETURNED", "REJECTED", "CANCELLED").contains(deliveryStatus)) {
            Optional<Invoice> existing = invoiceRepository.findOriginalByOrderId(order.getId());
            if (existing.isPresent()) {
                result.put("invoiceCreated", false);
                result.put("requiresAdjustment", true);
                result.put("existingInvoiceCode", existing.get().getInvoiceCode());
                result.put("message", "Đơn hàng đã xuất hóa đơn " + existing.get().getInvoiceCode()
                        + ". Vui lòng lập hóa đơn điều chỉnh/thay thế theo quy trình hoàn trả, không xóa lịch sử hóa đơn.");
                return result;
            }
            result.put("invoiceCreated", false);
            result.put("message", "Giao hàng không thành công (" + deliveryStatus + ") - không phát sinh hóa đơn");
            return result;
        }

        result.put("invoiceCreated", false);
        result.put("message", "Trạng thái vận chuyển " + deliveryStatus + " chưa đủ điều kiện phát sinh hóa đơn");
        return result;
    }

    // ==================== MAPPERS & HELPERS ====================

    private InvoiceDto toSummaryDto(Invoice inv) {
        InvoiceDto dto = InvoiceDto.builder()
                .id(inv.getId())
                .invoiceCode(inv.getInvoiceCode())
                .orderId(inv.getSalesOrder() != null ? inv.getSalesOrder().getId() : null)
                .orderCode(inv.getSalesOrder() != null ? inv.getSalesOrder().getOrderCode() : null)
                .originalInvoiceId(inv.getOriginalInvoice() != null ? inv.getOriginalInvoice().getId() : null)
                .originalInvoiceCode(inv.getOriginalInvoice() != null ? inv.getOriginalInvoice().getInvoiceCode() : null)
                .saleChannel(inv.getSaleChannel())
                .invoiceType(inv.getInvoiceType())
                .recordKind(inv.getRecordKind())
                .customerId(inv.getCustomer() != null ? inv.getCustomer().getId() : null)
                .customerName(inv.getCustomerName())
                .customerPhone(inv.getCustomerPhone())
                .customerEmail(inv.getCustomerEmail())
                .customerAddress(inv.getCustomerAddress())
                .companyName(inv.getCompanyName())
                .taxCode(inv.getTaxCode())
                .subtotal(inv.getSubtotal())
                .discountAmount(inv.getDiscountAmount())
                .shippingFee(inv.getShippingFee())
                .taxableAmount(inv.getTaxableAmount())
                .taxRate(inv.getTaxRate())
                .taxAmount(inv.getTaxAmount())
                .totalAmount(inv.getTotalAmount())
                .paymentMethod(inv.getPaymentMethod())
                .paymentStatus(inv.getPaymentStatus())
                .invoiceStatus(inv.getInvoiceStatus())
                .emailStatus(inv.getEmailStatus())
                .issuedAt(inv.getIssuedAt())
                .cancelledAt(inv.getCancelledAt())
                .cancelReason(inv.getCancelReason())
                .cancelledBy(inv.getCancelledBy())
                .adjustmentReason(inv.getAdjustmentReason())
                .note(inv.getNote())
                .createdBy(inv.getCreatedBy())
                .updatedBy(inv.getUpdatedBy())
                .build();

        if ("PENDING_CANCEL".equalsIgnoreCase(inv.getInvoiceStatus())) {
            cancelRequestRepository.findFirstByInvoiceIdAndStatusOrderByRequestedAtDesc(inv.getId(), "PENDING")
                    .ifPresent(req -> {
                        dto.setPendingCancelRequestId(req.getId());
                        dto.setPendingCancelReason(req.getReason());
                        dto.setPendingCancelRequestedBy(req.getRequestedBy());
                    });
        }
        return dto;
    }

    private InvoiceDto toFullDto(Invoice inv) {
        InvoiceDto dto = toSummaryDto(inv);
        dto.setDetails(detailRepository.findByInvoiceIdOrderByIdAsc(inv.getId()).stream()
                .map(d -> InvoiceDto.DetailItem.builder()
                        .id(d.getId())
                        .productId(d.getProductId())
                        .productVariantId(d.getProductVariantId())
                        .productCode(d.getProductCode())
                        .productName(d.getProductName())
                        .sku(d.getSku())
                        .variantLabel(d.getVariantLabel())
                        .quantity(d.getQuantity())
                        .unitPrice(d.getUnitPrice())
                        .discountAmount(d.getDiscountAmount())
                        .taxRate(d.getTaxRate())
                        .taxAmount(d.getTaxAmount())
                        .lineTotal(d.getLineTotal())
                        .build())
                .collect(Collectors.toList()));

        dto.setHistories(historyRepository.findByInvoiceIdOrderByCreatedAtDescIdDesc(inv.getId()).stream()
                .map(h -> InvoiceDto.HistoryItem.builder()
                        .id(h.getId())
                        .action(h.getAction())
                        .fromStatus(h.getFromStatus())
                        .toStatus(h.getToStatus())
                        .actor(h.getActor())
                        .reason(h.getReason())
                        .detail(h.getDetail())
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList()));

        dto.setCancelRequests(cancelRequestRepository.findByInvoiceIdOrderByRequestedAtDescIdDesc(inv.getId()).stream()
                .map(r -> InvoiceDto.CancelRequestItem.builder()
                        .id(r.getId())
                        .requestedBy(r.getRequestedBy())
                        .reason(r.getReason())
                        .status(r.getStatus())
                        .reviewedBy(r.getReviewedBy())
                        .reviewNote(r.getReviewNote())
                        .requestedAt(r.getRequestedAt())
                        .reviewedAt(r.getReviewedAt())
                        .build())
                .collect(Collectors.toList()));

        dto.setEmailHistories(emailHistoryRepository.findByInvoiceIdOrderByCreatedAtDescIdDesc(inv.getId()).stream()
                .map(e -> InvoiceDto.EmailHistoryItem.builder()
                        .id(e.getId())
                        .recipientEmail(e.getRecipientEmail())
                        .subject(e.getSubject())
                        .sendStatus(e.getSendStatus())
                        .sentBy(e.getSentBy())
                        .sentAt(e.getSentAt())
                        .retryCount(e.getRetryCount())
                        .errorMessage(e.getErrorMessage())
                        .createdAt(e.getCreatedAt())
                        .build())
                .collect(Collectors.toList()));

        dto.setAdjustmentInvoices(invoiceRepository.findByOriginalInvoiceIdOrderByIssuedAtDesc(inv.getId()).stream()
                .map(adj -> InvoiceDto.RelatedInvoiceItem.builder()
                        .id(adj.getId())
                        .invoiceCode(adj.getInvoiceCode())
                        .recordKind(adj.getRecordKind())
                        .invoiceType(adj.getInvoiceType())
                        .invoiceStatus(adj.getInvoiceStatus())
                        .totalAmount(adj.getTotalAmount())
                        .issuedAt(adj.getIssuedAt())
                        .build())
                .collect(Collectors.toList()));

        return dto;
    }

    private String resolveStaffScopeUsername() {
        if (SecurityUtils.isAdminOrManager()) {
            return null;
        }
        return SecurityUtils.getCurrentUsername();
    }

    private void enforceViewAccess(Invoice invoice) {
        if (SecurityUtils.isAdminOrManager()) {
            return;
        }
        String current = SecurityUtils.getCurrentUsername();
        if (current.equalsIgnoreCase(invoice.getCreatedBy())) return;
        SalesOrder so = invoice.getSalesOrder();
        if (so != null) {
            if (current.equalsIgnoreCase(so.getCreatedBy())) return;
            if (so.getEmployee() != null && so.getEmployee().getUser() != null
                    && current.equalsIgnoreCase(so.getEmployee().getUser().getUsername())) {
                return;
            }
        }
        throw new AccessDeniedException("Bạn chỉ được phép xem các hóa đơn thuộc phạm vi được phân công");
    }

    private void validateVatFieldsIfNeeded(String invoiceType, String companyName, String taxCode, String email) {
        if ("VAT".equalsIgnoreCase(invoiceType)) {
            if (companyName == null || companyName.isBlank()) {
                throw new BusinessException("Hóa đơn VAT bắt buộc nhập Tên công ty / Đơn vị mua hàng");
            }
            if (taxCode == null || taxCode.isBlank()) {
                throw new BusinessException("Hóa đơn VAT bắt buộc nhập Mã số thuế (MST)");
            }
        }
        if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new BusinessException("Địa chỉ Email không đúng định dạng: " + email);
        }
    }

    private String normalizeInvoiceType(String raw) {
        if (raw != null && "VAT".equalsIgnoreCase(raw.trim())) {
            return "VAT";
        }
        return "RETAIL";
    }

    private BigDecimal normalizeTaxRate(BigDecimal rate) {
        if (rate == null) return configuredDefaultTaxRate != null ? configuredDefaultTaxRate : BigDecimal.valueOf(8);
        BigDecimal stripped = rate.stripTrailingZeros();
        for (BigDecimal allowed : ALLOWED_TAX_RATES) {
            if (allowed.compareTo(stripped) == 0) return allowed;
        }
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("Thuế suất GTGT không hợp lệ: " + rate + "%");
        }
        return rate.setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveOrderPaymentStatus(SalesOrder order) {
        List<Payment> payments = paymentRepository.findBySalesOrderIdOrderByIdAsc(order.getId());
        boolean hasPaid = payments.stream().anyMatch(p -> "PAID".equalsIgnoreCase(p.getStatus()));
        if (hasPaid || AppConstants.ORDER_COMPLETED.equalsIgnoreCase(order.getStatus())) {
            return "PAID";
        }
        return "UNPAID";
    }

    private String generateInvoiceCode(String invoiceType, String recordKind) {
        String prefix = "ORIGINAL".equals(recordKind)
                ? ("VAT".equals(invoiceType) ? "VAT" : "INV")
                : ("REPLACEMENT".equals(recordKind) ? "REP" : "ADJ");
        String datePart = LocalDateTime.now().format(CODE_DATE_FMT);
        String code;
        do {
            int rand = ThreadLocalRandom.current().nextInt(1000, 10000);
            code = prefix + "-" + datePart + "-" + rand;
        } while (invoiceRepository.existsByInvoiceCodeIgnoreCase(code));
        return code;
    }

    private String extractFromOrderNote(String note, String label) {
        if (note == null || note.isBlank()) return null;
        Pattern p = Pattern.compile(Pattern.quote(label) + ":\\s*([^|]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(note);
        return m.find() ? m.group(1).trim() : null;
    }

    private String trimOrNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String upperOrNull(String s) {
        return (s == null || s.isBlank() || "ALL".equalsIgnoreCase(s.trim())) ? null : s.trim().toUpperCase();
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }

    private long toLong(Object obj) {
        return obj instanceof Number ? ((Number) obj).longValue() : 0L;
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj instanceof BigDecimal bd) return bd;
        if (obj instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }
}
