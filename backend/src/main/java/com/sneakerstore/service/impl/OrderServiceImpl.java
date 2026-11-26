package com.sneakerstore.service.impl;

import com.sneakerstore.common.constant.AppConstants;
import com.sneakerstore.common.exception.BusinessException;
import com.sneakerstore.common.exception.ResourceNotFoundException;
import com.sneakerstore.common.response.PageResponse;
import com.sneakerstore.common.util.SecurityUtils;
import com.sneakerstore.entity.Customer;
import com.sneakerstore.repository.CustomerRepository;
import com.sneakerstore.entity.Employee;
import com.sneakerstore.repository.EmployeeRepository;
import com.sneakerstore.dto.request.InvoiceIssueRequest;
import com.sneakerstore.dto.request.SalesOrderRequest;
import com.sneakerstore.dto.response.SalesOrderDto;
import com.sneakerstore.exporter.OrderExcelExporter;
import com.sneakerstore.service.InvoiceService;
import com.sneakerstore.service.VoucherService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.sneakerstore.entity.OrderStatusHistory;
import com.sneakerstore.entity.SalesOrder;
import com.sneakerstore.entity.SalesOrderDetail;
import com.sneakerstore.mapper.OrderMapper;
import com.sneakerstore.repository.OrderStatusHistoryRepository;
import com.sneakerstore.repository.SalesOrderDetailRepository;
import com.sneakerstore.repository.SalesOrderRepository;
import com.sneakerstore.service.OrderService;
import com.sneakerstore.entity.Payment;
import com.sneakerstore.repository.PaymentRepository;
import com.sneakerstore.entity.ProductVariant;
import com.sneakerstore.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderDetailRepository detailRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final ProductVariantRepository variantRepository;
    private final OrderMapper orderMapper;
    private final OrderExcelExporter orderExcelExporter;
    private final ObjectProvider<InvoiceService> invoiceServiceProvider;
    private final VoucherService voucherService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SalesOrderDto> searchOrders(
            String keyword, String status, String paymentMethod, String channel,
            LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        // Bước 1: Chuẩn hóa khoảng thời gian lọc & từ khóa
        LocalDateTime start = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime end = toDate != null ? toDate.atTime(LocalTime.MAX) : null;

        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String altKeyword = buildAltOrderCodeKeyword(trimmedKeyword);
        String cleanStatus = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim()))
                ? status.trim().toUpperCase() : null;
        String cleanPayment = (paymentMethod != null && !paymentMethod.isBlank() && !"ALL".equalsIgnoreCase(paymentMethod.trim()))
                ? paymentMethod.trim().toUpperCase() : null;
        String cleanChannel = (channel != null && !channel.isBlank() && !"ALL".equalsIgnoreCase(channel.trim()))
                ? channel.trim().toUpperCase() : null;

        // Bước 2-3: Truy vấn phân trang trong Transaction ReadOnly
        Page<SalesOrderDto> page = salesOrderRepository
                .searchOrders(trimmedKeyword, altKeyword, cleanStatus, cleanPayment, cleanChannel, start, end, pageable)
                .map(orderMapper::toSummaryDto);

        // Bước 4: Batch-load chi tiết sản phẩm (Quick Glance) & bản ghi thanh toán (SePay / COD) tránh N+1
        List<Long> orderIds = page.getContent().stream()
                .map(SalesOrderDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!orderIds.isEmpty()) {
            Map<Long, List<SalesOrderDetail>> detailsByOrder = detailRepository
                    .findBySalesOrderIdInOrderByIdAsc(orderIds)
                    .stream()
                    .collect(Collectors.groupingBy(d -> d.getSalesOrder().getId(), LinkedHashMap::new, Collectors.toList()));

            Map<Long, List<Payment>> paymentsByOrder = paymentRepository
                    .findBySalesOrderIdInOrderByIdAsc(orderIds)
                    .stream()
                    .collect(Collectors.groupingBy(p -> p.getSalesOrder().getId(), LinkedHashMap::new, Collectors.toList()));

            for (SalesOrderDto dto : page.getContent()) {
                List<SalesOrderDetail> dList = detailsByOrder.getOrDefault(dto.getId(), Collections.emptyList());
                dto.setDetails(dList.stream().map(orderMapper::toDetailDto).collect(Collectors.toList()));

                List<Payment> pList = paymentsByOrder.getOrDefault(dto.getId(), Collections.emptyList());
                dto.setPayments(pList.stream().map(orderMapper::toPaymentDto).collect(Collectors.toList()));
            }
        }

        return PageResponse.of(page);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getStatusCounts(
            String keyword, String paymentMethod, String channel,
            LocalDate fromDate, LocalDate toDate) {
        LocalDateTime start = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime end = toDate != null ? toDate.atTime(LocalTime.MAX) : null;

        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String altKeyword = buildAltOrderCodeKeyword(trimmedKeyword);
        String cleanPayment = (paymentMethod != null && !paymentMethod.isBlank() && !"ALL".equalsIgnoreCase(paymentMethod.trim()))
                ? paymentMethod.trim().toUpperCase() : null;
        String cleanChannel = (channel != null && !channel.isBlank() && !"ALL".equalsIgnoreCase(channel.trim()))
                ? channel.trim().toUpperCase() : null;

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("ALL", 0L);
        counts.put(AppConstants.ORDER_PENDING, 0L);
        counts.put(AppConstants.ORDER_CONFIRMED, 0L);
        counts.put(AppConstants.ORDER_SHIPPING, 0L);
        counts.put(AppConstants.ORDER_COMPLETED, 0L);
        counts.put(AppConstants.ORDER_CANCELLED, 0L);

        List<Object[]> rows = salesOrderRepository.countOrdersByStatus(
                trimmedKeyword, altKeyword, cleanPayment, cleanChannel, start, end);

        long total = 0L;
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) continue;
            String st = row[0].toString().toUpperCase();
            long cnt = ((Number) row[1]).longValue();
            total += cnt;
            counts.put(st, cnt);
        }
        counts.put("ALL", total);
        return counts;
    }

    private String buildAltOrderCodeKeyword(String trimmedKeyword) {
        if (trimmedKeyword == null) return null;
        // Hỗ trợ tìm kiếm thông minh: nếu nhập hd019, hd19, 19, HD19 -> chuẩn hóa sang mã chuẩn HD0019
        if (trimmedKeyword.matches("(?i)^HD0*(\\d+)$")) {
            try {
                long num = Long.parseLong(trimmedKeyword.replaceAll("(?i)^HD0*", ""));
                return String.format("HD%04d", num);
            } catch (Exception ignored) {
            }
        } else if (trimmedKeyword.matches("^\\d+$")) {
            try {
                long num = Long.parseLong(trimmedKeyword);
                return String.format("HD%04d", num);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesOrderDto> getOrdersByCustomer(Long customerId) {
        return salesOrderRepository.findByCustomerIdOrderByOrderDateDesc(customerId).stream()
                .map(orderMapper::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SalesOrderDto getById(Long id) {
        // Bước 1: Kiểm tra tồn tại của đơn hàng
        SalesOrder order = salesOrderRepository.findWithHeaderById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + id));

        // Bước 5: Output Mapping (Header + Details + Payments + Status Histories)
        SalesOrderDto dto = orderMapper.toSummaryDto(order);

        dto.setDetails(detailRepository.findBySalesOrderIdOrderByIdAsc(id).stream()
                .map(orderMapper::toDetailDto)
                .collect(Collectors.toList()));

        dto.setPayments(paymentRepository.findBySalesOrderIdOrderByIdAsc(id).stream()
                .map(orderMapper::toPaymentDto)
                .collect(Collectors.toList()));

        dto.setHistories(historyRepository.findBySalesOrderIdOrderByChangedAtAscIdAsc(id).stream()
                .map(orderMapper::toHistoryDto)
                .collect(Collectors.toList()));

        return dto;
    }

    @Override
    @Transactional
    public SalesOrderDto createOrder(SalesOrderRequest request) {
        // Bước 1: Validation Logic (Khách hàng, nhân viên, trạng thái khởi tạo, phương thức thanh toán)
        String actor = SecurityUtils.getCurrentUsername();

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + request.getCustomerId()));
        }

        Employee employee = resolveEmployee(request.getEmployeeId(), actor);

        // Gộp các dòng biến thể trùng nhau trong giỏ hàng
        Map<Long, Integer> requestedQtyMap = new LinkedHashMap<>();
        for (SalesOrderRequest.SalesOrderItemRequest item : request.getItems()) {
            requestedQtyMap.merge(item.getProductVariantId(), item.getQuantity(), Integer::sum);
        }

        Map<Long, ProductVariant> variantMap = variantRepository.findByIdIn(requestedQtyMap.keySet()).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        String initialStatus = request.getStatus() != null && !request.getStatus().isBlank()
                ? request.getStatus().toUpperCase()
                : AppConstants.ORDER_COMPLETED;
        if (!List.of(AppConstants.ORDER_DRAFT, AppConstants.ORDER_PENDING, AppConstants.ORDER_CONFIRMED, AppConstants.ORDER_SHIPPING, AppConstants.ORDER_COMPLETED).contains(initialStatus)) {
            throw new BusinessException("Trạng thái khởi tạo đơn hàng không hợp lệ: " + initialStatus);
        }

        String paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod().toUpperCase() : AppConstants.PAYMENT_CASH;
        if (!List.of(AppConstants.PAYMENT_CASH, AppConstants.PAYMENT_BANK_TRANSFER).contains(paymentMethod)) {
            throw new BusinessException("Phương thức thanh toán không hợp lệ: " + paymentMethod);
        }

        // Bước 2-3: Thực thi Logic Cốt lõi (Kiểm tra tồn kho, trừ availableQuantity, tính tổng tiền)
        String orderCode = generateOrderCode();
        SalesOrder order = SalesOrder.builder()
                .orderCode(orderCode)
                .customer(customer)
                .employee(employee)
                .orderDate(LocalDateTime.now())
                .totalAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .paymentMethod(paymentMethod)
                .status(initialStatus)
                .note(request.getNote())
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<SalesOrderDetail> detailEntities = new ArrayList<>();
        List<ProductVariant> variantsToUpdate = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : requestedQtyMap.entrySet()) {
            Long variantId = entry.getKey();
            Integer qty = entry.getValue();
            ProductVariant pv = variantMap.get(variantId);
            if (pv == null) {
                throw new ResourceNotFoundException("Không tìm thấy biến thể sản phẩm với ID: " + variantId);
            }
            if (!"ACTIVE".equalsIgnoreCase(pv.getStatus())
                    || (pv.getProduct() != null && !"ACTIVE".equalsIgnoreCase(pv.getProduct().getStatus()))) {
                throw new BusinessException("Sản phẩm " + pv.getSku() + " hiện đã ngừng kinh doanh");
            }
            int available = pv.getAvailableQuantity() != null ? pv.getAvailableQuantity() : 0;
            if (available < qty) {
                throw new BusinessException("Sản phẩm SKU " + pv.getSku() + " chỉ còn " + available + " đôi có sẵn, không đủ số lượng bán (" + qty + ")");
            }

            pv.setAvailableQuantity(available - qty);
            pv.setUpdatedBy(actor);
            variantsToUpdate.add(pv);

            BigDecimal unitPrice = pv.getSalePrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            totalAmount = totalAmount.add(subtotal);

            detailEntities.add(SalesOrderDetail.builder()
                    .salesOrder(order)
                    .productVariant(pv)
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .createdBy(actor)
                    .updatedBy(actor)
                    .build());
        }

        variantRepository.saveAll(variantsToUpdate);

        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingFee(BigDecimal.ZERO);
        order.setFinalAmount(totalAmount);
        order.setDetails(detailEntities);
        SalesOrder savedOrder = salesOrderRepository.save(order);

        // Bước 3.5: Áp dụng Voucher tại quầy POS (nếu thu ngân nhập/chọn mã)
        BigDecimal finalAmount = totalAmount;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            BigDecimal discount = voucherService.applyVoucherToOrder(
                    savedOrder, request.getVoucherCode(), "POS", customer, totalAmount);
            finalAmount = totalAmount.subtract(discount).max(BigDecimal.ZERO);
            savedOrder.setDiscountAmount(discount);
            savedOrder.setFinalAmount(finalAmount);
            savedOrder = salesOrderRepository.save(savedOrder);
        }

        // Bước 4: Lưu bản ghi Thanh toán (Payment) & Lịch sử trạng thái (OrderStatusHistory / Audit Log)
        String paymentStatus = AppConstants.ORDER_COMPLETED.equals(initialStatus) ? "PAID" : "PENDING";
        Payment payment = Payment.builder()
                .paymentCode(generatePaymentCode())
                .salesOrder(savedOrder)
                .paymentMethod(paymentMethod)
                .amount(finalAmount)
                .paymentDate(LocalDateTime.now())
                .status(paymentStatus)
                .referenceNote(request.getPaymentReference() != null ? request.getPaymentReference() : "Thanh toán đơn hàng " + orderCode)
                .createdBy(actor)
                .updatedBy(actor)
                .build();
        paymentRepository.save(payment);

        historyRepository.save(OrderStatusHistory.builder()
                .salesOrder(savedOrder)
                .fromStatus(null)
                .toStatus(initialStatus)
                .note(request.getNote() != null && !request.getNote().isBlank() ? request.getNote() : "Tạo mới đơn hàng tại quầy")
                .changedAt(LocalDateTime.now())
                .createdBy(actor)
                .updatedBy(actor)
                .build());

        // Bước 4.5: Tự động phát sinh hóa đơn khi đơn bán tại quầy hoàn thành (COMPLETED)
        if (AppConstants.ORDER_COMPLETED.equals(initialStatus)) {
            InvoiceService invService = invoiceServiceProvider.getIfAvailable();
            if (invService != null) {
                InvoiceIssueRequest invCfg = null;
                if (request.getInvoiceType() != null || request.getVatCompanyName() != null
                        || request.getVatTaxCode() != null || request.getVatEmail() != null
                        || request.getVatTaxRate() != null || Boolean.TRUE.equals(request.getAutoSendInvoiceEmail())) {
                    invCfg = InvoiceIssueRequest.builder()
                            .orderId(savedOrder.getId())
                            .invoiceType(request.getInvoiceType())
                            .companyName(request.getVatCompanyName())
                            .taxCode(request.getVatTaxCode())
                            .customerAddress(request.getVatAddress())
                            .customerEmail(request.getVatEmail())
                            .defaultTaxRate(request.getVatTaxRate())
                            .autoSendEmail(request.getAutoSendInvoiceEmail())
                            .build();
                }
                invService.autoIssueForCompletedOrder(savedOrder.getId(), invCfg);
            }
            // Bước 4.6: Đánh giá quy tắc thưởng Voucher động khi đơn POS hoàn thành
            voucherService.evaluateAndAwardRulesForCompletedOrder(savedOrder.getId());
        }

        // Bước 5: Output Mapping
        return getById(savedOrder.getId());
    }

    @Override
    @Transactional
    public SalesOrderDto updateStatus(Long id, String targetStatus, String note) {
        // Bước 1: Validation Logic & State Machine (Kiểm tra luồng chuyển trạng thái hợp lệ)
        SalesOrder order = salesOrderRepository.findWithHeaderById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + id));

        String currentStatus = order.getStatus();
        String nextStatus = targetStatus != null ? targetStatus.trim().toUpperCase() : "";

        validateTransition(currentStatus, nextStatus);

        String actor = SecurityUtils.getCurrentUsername();

        // Bước 2-3: Thực thi Logic Cốt lõi (Hoàn trả tồn kho nếu hủy đơn, hoặc chốt thanh toán nếu hoàn thành)
        if (AppConstants.ORDER_CANCELLED.equals(nextStatus)) {
            List<SalesOrderDetail> details = detailRepository.findBySalesOrderIdOrderByIdAsc(id);
            List<ProductVariant> variantsToRestore = new ArrayList<>();
            for (SalesOrderDetail d : details) {
                ProductVariant pv = d.getProductVariant();
                pv.setAvailableQuantity((pv.getAvailableQuantity() != null ? pv.getAvailableQuantity() : 0) + d.getQuantity());
                pv.setUpdatedBy(actor);
                variantsToRestore.add(pv);
            }
            variantRepository.saveAll(variantsToRestore);

            List<Payment> payments = paymentRepository.findBySalesOrderIdOrderByIdAsc(id);
            for (Payment p : payments) {
                p.setStatus("CANCELLED");
                p.setUpdatedBy(actor);
            }
            paymentRepository.saveAll(payments);

            // Hoàn trả lượt sử dụng Voucher & khôi phục voucher cá nhân khi hủy đơn
            voucherService.refundVoucherForCancelledOrder(order.getId(), note);
        } else if (AppConstants.ORDER_COMPLETED.equals(nextStatus)) {
            List<Payment> payments = paymentRepository.findBySalesOrderIdOrderByIdAsc(id);
            for (Payment p : payments) {
                p.setStatus("PAID");
                p.setPaymentDate(LocalDateTime.now());
                p.setUpdatedBy(actor);
            }
            paymentRepository.saveAll(payments);
        }

        order.setStatus(nextStatus);
        order.setUpdatedBy(actor);
        salesOrderRepository.save(order);

        // Bước 4: Lưu Lịch sử chuyển trạng thái (Audit / History Log)
        historyRepository.save(OrderStatusHistory.builder()
                .salesOrder(order)
                .fromStatus(currentStatus)
                .toStatus(nextStatus)
                .note(note != null && !note.isBlank() ? note : "Chuyển trạng thái từ " + currentStatus + " sang " + nextStatus)
                .changedAt(LocalDateTime.now())
                .createdBy(actor)
                .updatedBy(actor)
                .build());

        // Bước 4.5: Tự động phát sinh hóa đơn khi đơn hàng (POS / Online) chuyển sang COMPLETED (Idempotent)
        if (AppConstants.ORDER_COMPLETED.equals(nextStatus)) {
            InvoiceService invService = invoiceServiceProvider.getIfAvailable();
            if (invService != null) {
                invService.autoIssueForCompletedOrder(order.getId(), null);
            }
            // Đánh giá quy tắc thưởng Voucher động & tự động gửi email chúc mừng
            voucherService.evaluateAndAwardRulesForCompletedOrder(order.getId());
        }

        // Bước 5: Output Mapping
        return getById(id);
    }

    @Override
    @Transactional
    public SalesOrderDto updateNote(Long id, String note) {
        SalesOrder order = salesOrderRepository.findWithHeaderById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + id));
        String actor = SecurityUtils.getCurrentUsername();
        order.setNote(note != null ? note.trim() : "");
        order.setUpdatedBy(actor);
        salesOrderRepository.save(order);
        return getById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportOrdersExcel(
            String keyword, String status, String paymentMethod, String channel,
            LocalDate fromDate, LocalDate toDate) {
        PageResponse<SalesOrderDto> page = searchOrders(
                keyword, status, paymentMethod, channel, fromDate, toDate,
                PageRequest.of(0, 2000, Sort.by(Sort.Direction.DESC, "id")));
        return orderExcelExporter.export(page.getContent() != null ? page.getContent() : Collections.emptyList());
    }

    private void validateTransition(String currentStatus, String nextStatus) {
        if (currentStatus.equals(nextStatus)) {
            throw new BusinessException("Đơn hàng đã ở trạng thái " + currentStatus + ", không thể thực hiện lặp lại");
        }
        if (AppConstants.ORDER_COMPLETED.equals(currentStatus)) {
            throw new BusinessException("Đơn hàng đã hoàn thành (COMPLETED), không thể thay đổi trạng thái hoặc hủy đơn");
        }
        if (AppConstants.ORDER_CANCELLED.equals(currentStatus)) {
            throw new BusinessException("Đơn hàng đã bị hủy (CANCELLED), không thể khôi phục hoặc chuyển trạng thái");
        }
        if (AppConstants.ORDER_DRAFT.equals(currentStatus) || AppConstants.ORDER_PENDING.equals(currentStatus)) {
            if (!List.of(AppConstants.ORDER_CONFIRMED, AppConstants.ORDER_SHIPPING, AppConstants.ORDER_COMPLETED, AppConstants.ORDER_CANCELLED).contains(nextStatus)) {
                throw new BusinessException("Luồng trạng thái không hợp lệ: Không thể chuyển từ " + currentStatus + " sang " + nextStatus);
            }
            return;
        }
        if (AppConstants.ORDER_CONFIRMED.equals(currentStatus)) {
            if (!List.of(AppConstants.ORDER_SHIPPING, AppConstants.ORDER_COMPLETED, AppConstants.ORDER_CANCELLED).contains(nextStatus)) {
                throw new BusinessException("Luồng trạng thái không hợp lệ: Từ CONFIRMED chỉ có thể chuyển sang SHIPPING, COMPLETED hoặc CANCELLED");
            }
            return;
        }
        if (AppConstants.ORDER_SHIPPING.equals(currentStatus)) {
            if (!List.of(AppConstants.ORDER_COMPLETED, AppConstants.ORDER_CANCELLED).contains(nextStatus)) {
                throw new BusinessException("Luồng trạng thái không hợp lệ: Từ SHIPPING chỉ có thể chuyển sang COMPLETED hoặc CANCELLED");
            }
            return;
        }
        throw new BusinessException("Trạng thái không hợp lệ: " + nextStatus);
    }

    private Employee resolveEmployee(Long employeeId, String username) {
        if (employeeId != null) {
            return employeeRepository.findById(employeeId).orElse(null);
        }
        return employeeRepository.findByUserUsername(username).orElse(null);
    }

    private String generateOrderCode() {
        String code;
        do {
            int rand = java.util.concurrent.ThreadLocalRandom.current().nextInt(100000, 1000000);
            code = "HD" + rand;
        } while (salesOrderRepository.existsByOrderCodeIgnoreCase(code));
        return code;
    }

    private String generatePaymentCode() {
        String code;
        do {
            int rand = java.util.concurrent.ThreadLocalRandom.current().nextInt(100000, 1000000);
            code = "TT" + rand;
        } while (paymentRepository.existsByPaymentCodeIgnoreCase(code));
        return code;
    }
}

