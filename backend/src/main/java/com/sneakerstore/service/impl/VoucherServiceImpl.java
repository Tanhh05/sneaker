package com.sneakerstore.service.impl;

import com.sneakerstore.common.constant.AppConstants;
import com.sneakerstore.common.exception.BusinessException;
import com.sneakerstore.common.exception.ResourceNotFoundException;
import com.sneakerstore.common.response.PageResponse;
import com.sneakerstore.common.util.SecurityUtils;
import com.sneakerstore.dto.request.VoucherRequest;
import com.sneakerstore.dto.request.VoucherRuleRequest;
import com.sneakerstore.dto.request.VoucherValidateRequest;
import com.sneakerstore.dto.response.*;
import com.sneakerstore.entity.*;
import com.sneakerstore.repository.*;
import com.sneakerstore.service.VoucherEmailSender;
import com.sneakerstore.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherRuleRepository voucherRuleRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final CustomerRepository customerRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final UserRepository userRepository;
    private final VoucherEmailSender voucherEmailSender;

    // =========================================================================
    // 1. VOUCHER MASTER DATA CRUD
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VoucherDto> searchVouchers(
            String keyword, String channel, String discountType,
            String voucherType, String status,
            LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        LocalDateTime start = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime end = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
        Page<VoucherDto> page = voucherRepository.searchVouchers(
                trimOrNull(keyword), upperOrNull(channel), upperOrNull(discountType),
                upperOrNull(voucherType), upperOrNull(status), start, end, pageable
        ).map(this::toVoucherDto);
        return PageResponse.of(page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherDto> getAllVouchers() {
        return voucherRepository.findByDeletedFalseOrderByIdDesc().stream()
                .map(this::toVoucherDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherDto getVoucherById(Long id) {
        Voucher v = voucherRepository.findById(id)
                .filter(x -> !x.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với ID: " + id));
        return toVoucherDto(v);
    }

    @Override
    @Transactional
    public VoucherDto createVoucher(VoucherRequest req) {
        validateVoucherRequest(req, null);
        String actor = SecurityUtils.getCurrentUsername();
        String normalizedCode = req.getCode().trim().toUpperCase();

        Voucher voucher = Voucher.builder()
                .code(normalizedCode)
                .name(req.getName().trim())
                .description(trimOrNull(req.getDescription()))
                .discountType(req.getDiscountType().trim().toUpperCase())
                .discountValue(req.getDiscountValue())
                .maxDiscountAmount("PERCENT".equalsIgnoreCase(req.getDiscountType()) ? req.getMaxDiscountAmount() : null)
                .minOrderAmount(req.getMinOrderAmount() != null ? req.getMinOrderAmount() : BigDecimal.ZERO)
                .totalQuantity(req.getTotalQuantity())
                .usedCount(0)
                .perCustomerLimit(req.getPerCustomerLimit() != null ? req.getPerCustomerLimit() : 1)
                .channel(req.getChannel().trim().toUpperCase())
                .voucherType(req.getVoucherType().trim().toUpperCase())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .status(req.getStatus() != null && !req.getStatus().isBlank() ? req.getStatus().trim().toUpperCase() : "ACTIVE")
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        return toVoucherDto(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public VoucherDto updateVoucher(Long id, VoucherRequest req) {
        Voucher voucher = voucherRepository.findById(id)
                .filter(x -> !x.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với ID: " + id));
        validateVoucherRequest(req, id);
        String actor = SecurityUtils.getCurrentUsername();

        voucher.setCode(req.getCode().trim().toUpperCase());
        voucher.setName(req.getName().trim());
        voucher.setDescription(trimOrNull(req.getDescription()));
        voucher.setDiscountType(req.getDiscountType().trim().toUpperCase());
        voucher.setDiscountValue(req.getDiscountValue());
        voucher.setMaxDiscountAmount("PERCENT".equalsIgnoreCase(req.getDiscountType()) ? req.getMaxDiscountAmount() : null);
        voucher.setMinOrderAmount(req.getMinOrderAmount() != null ? req.getMinOrderAmount() : BigDecimal.ZERO);
        voucher.setTotalQuantity(req.getTotalQuantity());
        voucher.setPerCustomerLimit(req.getPerCustomerLimit() != null ? req.getPerCustomerLimit() : 1);
        voucher.setChannel(req.getChannel().trim().toUpperCase());
        voucher.setVoucherType(req.getVoucherType().trim().toUpperCase());
        voucher.setStartDate(req.getStartDate());
        voucher.setEndDate(req.getEndDate());
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            voucher.setStatus(req.getStatus().trim().toUpperCase());
        }
        voucher.setUpdatedBy(actor);

        return toVoucherDto(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public VoucherDto toggleVoucherStatus(Long id, String status) {
        Voucher voucher = voucherRepository.findById(id)
                .filter(x -> !x.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với ID: " + id));
        String s = status != null ? status.trim().toUpperCase() : "INACTIVE";
        if (!List.of("ACTIVE", "INACTIVE", "EXPIRED").contains(s)) {
            throw new BusinessException("Trạng thái voucher không hợp lệ: " + s);
        }
        voucher.setStatus(s);
        voucher.setUpdatedBy(SecurityUtils.getCurrentUsername());
        return toVoucherDto(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public void softDeleteVoucher(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với ID: " + id));
        voucher.setDeleted(true);
        voucher.setStatus("INACTIVE");
        voucher.setUpdatedBy(SecurityUtils.getCurrentUsername());
        voucherRepository.save(voucher);
    }

    private void validateVoucherRequest(VoucherRequest req, Long excludeId) {
        String code = req.getCode().trim().toUpperCase();
        if (!code.matches("^[A-Z0-9_-]{3,50}$")) {
            throw new BusinessException("Mã voucher chỉ gồm chữ in hoa, số, gạch ngang/dưới (từ 3 đến 50 ký tự)");
        }
        boolean dup = excludeId == null
                ? voucherRepository.existsByCodeIgnoreCaseAndDeletedFalse(code)
                : voucherRepository.existsByCodeIgnoreCaseAndIdNotAndDeletedFalse(code, excludeId);
        if (dup) {
            throw new BusinessException("Mã voucher '" + code + "' đã tồn tại trong hệ thống!");
        }
        String dType = req.getDiscountType().trim().toUpperCase();
        if (!List.of("PERCENT", "FIXED").contains(dType)) {
            throw new BusinessException("Hình thức giảm giá phải là PERCENT hoặc FIXED");
        }
        if ("PERCENT".equals(dType) && req.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("Giá trị giảm theo phần trăm không được vượt quá 100%");
        }
        if (!List.of("ALL", "POS", "ONLINE").contains(req.getChannel().trim().toUpperCase())) {
            throw new BusinessException("Kênh áp dụng phải là ALL, POS hoặc ONLINE");
        }
        if (!List.of("PUBLIC", "PRIVATE").contains(req.getVoucherType().trim().toUpperCase())) {
            throw new BusinessException("Loại voucher phải là PUBLIC hoặc PRIVATE");
        }
        if (!req.getEndDate().isAfter(req.getStartDate())) {
            throw new BusinessException("Ngày kết thúc hiệu lực phải sau ngày bắt đầu");
        }
    }

    // =========================================================================
    // 2. DYNAMIC TRIGGER RULES CRUD
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<VoucherRuleDto> getAllRules() {
        return voucherRuleRepository.findByDeletedFalseOrderByIdDesc().stream()
                .map(this::toRuleDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VoucherRuleDto createRule(VoucherRuleRequest req) {
        validateRuleRequest(req);
        Voucher voucher = voucherRepository.findById(req.getVoucherId())
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher phần thưởng ID: " + req.getVoucherId()));
        String actor = SecurityUtils.getCurrentUsername();

        VoucherRule rule = VoucherRule.builder()
                .ruleName(req.getRuleName().trim())
                .description(trimOrNull(req.getDescription()))
                .triggerType(req.getTriggerType().trim().toUpperCase())
                .thresholdValue(req.getThresholdValue())
                .voucher(voucher)
                .maxAwardsPerCustomer(req.getMaxAwardsPerCustomer())
                .validityDays(req.getValidityDays())
                .status(req.getStatus() != null && !req.getStatus().isBlank() ? req.getStatus().trim().toUpperCase() : "ACTIVE")
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        return toRuleDto(voucherRuleRepository.save(rule));
    }

    @Override
    @Transactional
    public VoucherRuleDto updateRule(Long id, VoucherRuleRequest req) {
        validateRuleRequest(req);
        VoucherRule rule = voucherRuleRepository.findWithVoucherById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy tắc thưởng ID: " + id));
        Voucher voucher = voucherRepository.findById(req.getVoucherId())
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher phần thưởng ID: " + req.getVoucherId()));
        String actor = SecurityUtils.getCurrentUsername();

        rule.setRuleName(req.getRuleName().trim());
        rule.setDescription(trimOrNull(req.getDescription()));
        rule.setTriggerType(req.getTriggerType().trim().toUpperCase());
        rule.setThresholdValue(req.getThresholdValue());
        rule.setVoucher(voucher);
        rule.setMaxAwardsPerCustomer(req.getMaxAwardsPerCustomer());
        rule.setValidityDays(req.getValidityDays());
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            rule.setStatus(req.getStatus().trim().toUpperCase());
        }
        rule.setUpdatedBy(actor);

        return toRuleDto(voucherRuleRepository.save(rule));
    }

    @Override
    @Transactional
    public VoucherRuleDto toggleRuleStatus(Long id, String status) {
        VoucherRule rule = voucherRuleRepository.findWithVoucherById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy tắc thưởng ID: " + id));
        String s = status != null ? status.trim().toUpperCase() : "INACTIVE";
        if (!List.of("ACTIVE", "INACTIVE").contains(s)) {
            throw new BusinessException("Trạng thái quy tắc không hợp lệ: " + s);
        }
        rule.setStatus(s);
        rule.setUpdatedBy(SecurityUtils.getCurrentUsername());
        return toRuleDto(voucherRuleRepository.save(rule));
    }

    @Override
    @Transactional
    public void softDeleteRule(Long id) {
        VoucherRule rule = voucherRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy tắc thưởng ID: " + id));
        rule.setDeleted(true);
        rule.setStatus("INACTIVE");
        rule.setUpdatedBy(SecurityUtils.getCurrentUsername());
        voucherRuleRepository.save(rule);
    }

    private void validateRuleRequest(VoucherRuleRequest req) {
        String t = req.getTriggerType().trim().toUpperCase();
        if (!List.of("ORDER_COUNT", "TOTAL_SPENT", "SINGLE_ORDER_AMOUNT").contains(t)) {
            throw new BusinessException("Loại kích hoạt phải là ORDER_COUNT, TOTAL_SPENT hoặc SINGLE_ORDER_AMOUNT");
        }
    }

    // =========================================================================
    // 3. USAGE HISTORY & AVAILABLE VOUCHERS (POS + ONLINE WALLET)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VoucherUsageDto> searchUsages(String keyword, String channel, String status, Pageable pageable) {
        Page<VoucherUsageDto> page = voucherUsageRepository.searchUsages(
                trimOrNull(keyword), upperOrNull(channel), upperOrNull(status), pageable
        ).map(this::toUsageDto);
        return PageResponse.of(page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerVoucherDto> getAvailableVouchers(
            String channel, Long customerId, String phone, String email, BigDecimal orderAmount) {
        String cleanChannel = (channel != null && !channel.isBlank()) ? channel.trim().toUpperCase() : "ONLINE";
        BigDecimal subtotal = orderAmount != null && orderAmount.compareTo(BigDecimal.ZERO) > 0
                ? orderAmount : BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        Set<Long> resolvedCustomerIds = resolveCustomerIds(customerId, phone, email);
        List<CustomerVoucherDto> result = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();

        // 1. Personal Wallet Vouchers (CustomerVoucher)
        for (Long cid : resolvedCustomerIds) {
            List<CustomerVoucher> personalList = customerVoucherRepository.findByCustomerIdAndStatusOrderByExpiresAtAsc(cid, "AVAILABLE");
            for (CustomerVoucher cv : personalList) {
                if (cv.getExpiresAt() == null || !cv.getExpiresAt().isAfter(now)) continue;
                Voucher v = cv.getVoucher();
                if (v == null || v.isDeleted() || !"ACTIVE".equalsIgnoreCase(v.getStatus())) continue;
                if (!isChannelAllowed(v.getChannel(), cleanChannel)) continue;
                if (v.getTotalQuantity() != null && v.getUsedCount() >= v.getTotalQuantity()) continue;
                if (!seenCodes.add(cv.getPersonalCode().toUpperCase())) continue;

                result.add(buildAvailableCard(v, cv, subtotal, now));
            }
        }

        // 2. Public Active Vouchers for this channel
        List<Voucher> publicVouchers = voucherRepository.findActivePublicVouchersForChannel(cleanChannel, now);
        for (Voucher v : publicVouchers) {
            // Check per-customer usage limit across all matched customer IDs
            boolean limitReached = false;
            if (!resolvedCustomerIds.isEmpty() && v.getPerCustomerLimit() != null) {
                long usedByCust = 0;
                for (Long cid : resolvedCustomerIds) {
                    usedByCust += voucherUsageRepository.countByVoucherIdAndCustomerIdAndStatus(v.getId(), cid, "APPLIED");
                }
                if (usedByCust >= v.getPerCustomerLimit()) {
                    limitReached = true;
                }
            }
            if (limitReached) continue;
            if (!seenCodes.add(v.getCode().toUpperCase())) continue;

            result.add(buildAvailableCard(v, null, subtotal, now));
        }

        // Sort: eligible first -> highest discount -> personal first -> soonest expiry
        result.sort(Comparator
                .comparing(CustomerVoucherDto::isEligible).reversed()
                .thenComparing(CustomerVoucherDto::getEstimatedDiscount, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(dto -> "PERSONAL".equals(dto.getSourceType()) ? 0 : 1)
                .thenComparing(CustomerVoucherDto::getExpiresAt, Comparator.nullsLast(Comparator.naturalOrder())));

        return result;
    }

    private CustomerVoucherDto buildAvailableCard(Voucher v, CustomerVoucher cv, BigDecimal subtotal, LocalDateTime now) {
        LocalDateTime exp = cv != null ? cv.getExpiresAt() : v.getEndDate();
        long daysLeft = exp != null ? Math.max(0, ChronoUnit.DAYS.between(now.toLocalDate(), exp.toLocalDate())) : 0;

        BigDecimal minOrder = v.getMinOrderAmount() != null ? v.getMinOrderAmount() : BigDecimal.ZERO;
        boolean eligible = subtotal.compareTo(minOrder) >= 0;
        BigDecimal missing = eligible ? BigDecimal.ZERO : minOrder.subtract(subtotal);
        BigDecimal estDiscount = calculateDiscount(v, eligible ? subtotal : minOrder);
        String reason = eligible ? null : ("Cần mua thêm " + formatVnd(missing) + " để áp dụng mã này");

        return CustomerVoucherDto.builder()
                .id(cv != null ? cv.getId() : null)
                .voucherId(v.getId())
                .code(cv != null ? cv.getPersonalCode() : v.getCode())
                .baseVoucherCode(v.getCode())
                .name(v.getName())
                .description(v.getDescription())
                .discountType(v.getDiscountType())
                .discountValue(v.getDiscountValue())
                .maxDiscountAmount(v.getMaxDiscountAmount())
                .minOrderAmount(minOrder)
                .channel(v.getChannel())
                .voucherType(v.getVoucherType())
                .sourceType(cv != null ? "PERSONAL" : "PUBLIC")
                .status(cv != null ? cv.getStatus() : v.getStatus())
                .issuedAt(cv != null ? cv.getIssuedAt() : v.getStartDate())
                .expiresAt(exp)
                .remainingDays(daysLeft)
                .ruleName(cv != null && cv.getRule() != null ? cv.getRule().getRuleName() : null)
                .eligible(eligible)
                .estimatedDiscount(eligible ? estDiscount : BigDecimal.ZERO)
                .missingAmount(missing)
                .ineligibleReason(reason)
                .build();
    }

    // =========================================================================
    // 4. VALIDATION & APPLICATION ENGINE (POS + ONLINE)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public VoucherValidationResponse validateVoucher(VoucherValidateRequest request) {
        BigDecimal orderAmount = request.getOrderAmount() != null ? request.getOrderAmount().max(BigDecimal.ZERO) : BigDecimal.ZERO;
        if (request.getVoucherCode() == null || request.getVoucherCode().isBlank()) {
            return invalid("Vui lòng nhập mã voucher", orderAmount, BigDecimal.ZERO);
        }

        String rawCode = request.getVoucherCode().trim().toUpperCase();
        String channel = (request.getChannel() != null && !request.getChannel().isBlank())
                ? request.getChannel().trim().toUpperCase() : "ONLINE";
        LocalDateTime now = LocalDateTime.now();

        Set<Long> resolvedCustomerIds = resolveCustomerIds(
                request.getCustomerId(), request.getCustomerPhone(), request.getCustomerEmail());

        // Step 1: Resolve either Personal CustomerVoucher or Master Voucher
        CustomerVoucher matchedCv = customerVoucherRepository.findByPersonalCodeIgnoreCase(rawCode).orElse(null);
        Voucher voucher;

        if (matchedCv != null) {
            voucher = matchedCv.getVoucher();
            if (voucher == null || voucher.isDeleted()) {
                return invalid("Mã voucher không còn tồn tại trong hệ thống", orderAmount, BigDecimal.ZERO);
            }
            // Ownership check for personal voucher
            if (resolvedCustomerIds.isEmpty() || !resolvedCustomerIds.contains(matchedCv.getCustomer().getId())) {
                return invalid("Mã voucher cá nhân này không thuộc về khách hàng hiện tại!", orderAmount, BigDecimal.ZERO);
            }
            if (!"AVAILABLE".equalsIgnoreCase(matchedCv.getStatus())) {
                return invalid("Mã voucher cá nhân này đã được sử dụng hoặc đã hết hiệu lực!", orderAmount, BigDecimal.ZERO);
            }
            if (matchedCv.getExpiresAt() == null || !matchedCv.getExpiresAt().isAfter(now)) {
                return invalid("Mã voucher cá nhân này đã hết hạn sử dụng!", orderAmount, BigDecimal.ZERO);
            }
        } else {
            voucher = voucherRepository.findByCodeIgnoreCaseAndDeletedFalse(rawCode).orElse(null);
            if (voucher == null) {
                return invalid("Mã voucher '" + rawCode + "' không tồn tại hoặc đã bị vô hiệu hóa!", orderAmount, BigDecimal.ZERO);
            }
            // If PRIVATE master voucher, customer MUST possess an AVAILABLE CustomerVoucher for it
            if ("PRIVATE".equalsIgnoreCase(voucher.getVoucherType())) {
                for (Long cid : resolvedCustomerIds) {
                    Optional<CustomerVoucher> owned = customerVoucherRepository
                            .findFirstByCustomerIdAndVoucherIdAndStatusAndExpiresAtAfterOrderByExpiresAtAsc(
                                    cid, voucher.getId(), "AVAILABLE", now);
                    if (owned.isPresent()) {
                        matchedCv = owned.get();
                        break;
                    }
                }
                if (matchedCv == null) {
                    return invalid("Mã voucher '" + rawCode + "' là ưu đãi cá nhân dành riêng cho khách hàng đủ điều kiện!", orderAmount, BigDecimal.ZERO);
                }
            }
        }

        // Step 2: Check voucher active status & time window
        if (!"ACTIVE".equalsIgnoreCase(voucher.getStatus())) {
            return invalid("Mã voucher '" + rawCode + "' hiện đang tạm ngừng hoạt động!", orderAmount, BigDecimal.ZERO);
        }
        if (matchedCv == null) {
            if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
                return invalid("Mã voucher '" + rawCode + "' chưa đến thời gian áp dụng!", orderAmount, BigDecimal.ZERO);
            }
            if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
                return invalid("Mã voucher '" + rawCode + "' đã hết hạn sử dụng!", orderAmount, BigDecimal.ZERO);
            }
        }

        // Step 3: Check channel eligibility
        if (!isChannelAllowed(voucher.getChannel(), channel)) {
            String target = "POS".equalsIgnoreCase(voucher.getChannel()) ? "mua trực tiếp tại quầy (POS)" : "đặt hàng trực tuyến (Online)";
            return invalid("Mã voucher '" + rawCode + "' chỉ áp dụng cho kênh " + target + "!", orderAmount, BigDecimal.ZERO);
        }

        // Step 4: Check global quota
        if (voucher.getTotalQuantity() != null && voucher.getUsedCount() >= voucher.getTotalQuantity()) {
            return invalid("Mã voucher '" + rawCode + "' đã hết lượt phát hành trên toàn hệ thống!", orderAmount, BigDecimal.ZERO);
        }

        // Step 5: Check per-customer usage limit
        if (matchedCv == null && !resolvedCustomerIds.isEmpty() && voucher.getPerCustomerLimit() != null) {
            long usedByCust = 0;
            for (Long cid : resolvedCustomerIds) {
                usedByCust += voucherUsageRepository.countByVoucherIdAndCustomerIdAndStatus(voucher.getId(), cid, "APPLIED");
            }
            if (usedByCust >= voucher.getPerCustomerLimit()) {
                return invalid("Bạn đã sử dụng hết số lần cho phép (" + voucher.getPerCustomerLimit() + " lần) của mã '" + rawCode + "'!", orderAmount, BigDecimal.ZERO);
            }
        }

        // Step 6: Check minimum order amount
        BigDecimal minOrder = voucher.getMinOrderAmount() != null ? voucher.getMinOrderAmount() : BigDecimal.ZERO;
        if (orderAmount.compareTo(minOrder) < 0) {
            BigDecimal missing = minOrder.subtract(orderAmount);
            return invalid("Cần mua thêm " + formatVnd(missing) + " để áp dụng mã '" + rawCode + "' (đơn tối thiểu " + formatVnd(minOrder) + ")", orderAmount, missing);
        }

        // Step 7: Calculate discount
        BigDecimal discount = calculateDiscount(voucher, orderAmount);
        BigDecimal finalAmt = orderAmount.subtract(discount).max(BigDecimal.ZERO);

        return VoucherValidationResponse.builder()
                .valid(true)
                .message("Áp dụng mã voucher thành công — Giảm " + formatVnd(discount))
                .voucherId(voucher.getId())
                .customerVoucherId(matchedCv != null ? matchedCv.getId() : null)
                .appliedCode(matchedCv != null ? matchedCv.getPersonalCode() : voucher.getCode())
                .voucherName(voucher.getName())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderAmount(minOrder)
                .orderAmount(orderAmount)
                .discountAmount(discount)
                .finalAmount(finalAmt)
                .missingAmount(BigDecimal.ZERO)
                .build();
    }

    @Override
    @Transactional
    public BigDecimal applyVoucherToOrder(
            SalesOrder order, String voucherCode, String channel, Customer customer, BigDecimal orderSubtotal) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return BigDecimal.ZERO;
        }

        VoucherValidateRequest req = new VoucherValidateRequest();
        req.setVoucherCode(voucherCode.trim());
        req.setChannel(channel);
        req.setOrderAmount(orderSubtotal);
        if (customer != null) {
            req.setCustomerId(customer.getId());
            req.setCustomerPhone(customer.getPhone());
            req.setCustomerEmail(customer.getEmail());
        }

        VoucherValidationResponse validation = validateVoucher(req);
        if (!validation.isValid()) {
            throw new BusinessException(validation.getMessage());
        }

        Voucher voucher = voucherRepository.findById(validation.getVoucherId())
                .orElseThrow(() -> new BusinessException("Voucher không tồn tại"));

        // Re-check global quota inside write tx
        if (voucher.getTotalQuantity() != null && voucher.getUsedCount() >= voucher.getTotalQuantity()) {
            throw new BusinessException("Mã voucher '" + voucher.getCode() + "' vừa hết lượt sử dụng!");
        }

        voucher.setUsedCount((voucher.getUsedCount() != null ? voucher.getUsedCount() : 0) + 1);
        voucherRepository.save(voucher);

        CustomerVoucher cv = null;
        if (validation.getCustomerVoucherId() != null) {
            cv = customerVoucherRepository.findById(validation.getCustomerVoucherId())
                    .orElseThrow(() -> new BusinessException("Voucher cá nhân không tồn tại"));
            if (!"AVAILABLE".equalsIgnoreCase(cv.getStatus())) {
                throw new BusinessException("Voucher cá nhân này đã được sử dụng!");
            }
            cv.setStatus("USED");
            cv.setUsedAt(LocalDateTime.now());
            cv.setUsedOrder(order);
            customerVoucherRepository.save(cv);
        }

        BigDecimal discount = validation.getDiscountAmount();
        order.setVoucher(voucher);
        order.setVoucherCode(validation.getAppliedCode());
        order.setDiscountAmount(discount);

        String actor = SecurityUtils.getCurrentUsername();
        voucherUsageRepository.save(VoucherUsage.builder()
                .voucher(voucher)
                .customerVoucher(cv)
                .salesOrder(order)
                .customer(customer)
                .voucherCode(validation.getAppliedCode())
                .channel(channel != null ? channel.trim().toUpperCase() : "ONLINE")
                .orderSubtotal(orderSubtotal)
                .discountAmount(discount)
                .status("APPLIED")
                .usedAt(LocalDateTime.now())
                .note("Áp dụng thành công cho đơn hàng " + order.getOrderCode())
                .createdBy(actor)
                .updatedBy(actor)
                .build());

        return discount;
    }

    // =========================================================================
    // 5. CANCELLED ORDER -> REFUND VOUCHER QUOTA & RESTORE PERSONAL VOUCHER
    // =========================================================================

    @Override
    @Transactional
    public void refundVoucherForCancelledOrder(Long salesOrderId, String reason) {
        Optional<VoucherUsage> usageOpt = voucherUsageRepository.findFirstBySalesOrderIdAndStatus(salesOrderId, "APPLIED");
        if (usageOpt.isEmpty()) {
            return;
        }

        VoucherUsage usage = usageOpt.get();
        LocalDateTime now = LocalDateTime.now();
        String actor = SecurityUtils.getCurrentUsername();

        usage.setStatus("REFUNDED");
        usage.setRefundedAt(now);
        usage.setNote("Hoàn lượt voucher do hủy đơn" + (reason != null && !reason.isBlank() ? ": " + reason : ""));
        usage.setUpdatedBy(actor);
        voucherUsageRepository.save(usage);

        Voucher voucher = usage.getVoucher();
        if (voucher != null && voucher.getUsedCount() != null && voucher.getUsedCount() > 0) {
            voucher.setUsedCount(voucher.getUsedCount() - 1);
            voucherRepository.save(voucher);
        }

        CustomerVoucher cv = usage.getCustomerVoucher();
        if (cv != null) {
            if (cv.getExpiresAt() != null && cv.getExpiresAt().isAfter(now)) {
                cv.setStatus("AVAILABLE");
            } else {
                cv.setStatus("EXPIRED");
            }
            cv.setUsedAt(null);
            cv.setUsedOrder(null);
            cv.setUpdatedBy(actor);
            customerVoucherRepository.save(cv);
        }

        log.info("[VoucherService] Đã hoàn trả voucher {} cho đơn hàng ID {}", usage.getVoucherCode(), salesOrderId);
    }

    // =========================================================================
    // 6. DYNAMIC TRIGGER RULES EVALUATION ON COMPLETED ORDER
    // =========================================================================

    @Override
    @Transactional
    public List<CustomerVoucherDto> evaluateAndAwardRulesForCompletedOrder(Long salesOrderId) {
        SalesOrder order = salesOrderRepository.findWithHeaderById(salesOrderId).orElse(null);
        if (order == null || !AppConstants.ORDER_COMPLETED.equalsIgnoreCase(order.getStatus())) {
            return Collections.emptyList();
        }
        Customer customer = order.getCustomer();
        if (customer == null) {
            return Collections.emptyList();
        }

        List<VoucherRule> activeRules = voucherRuleRepository.findByStatusAndDeletedFalseOrderByThresholdValueAsc("ACTIVE");
        if (activeRules.isEmpty()) {
            return Collections.emptyList();
        }

        long completedCount = salesOrderRepository.countCompletedByCustomerId(customer.getId());
        BigDecimal totalSpent = salesOrderRepository.sumCompletedFinalAmountByCustomerId(customer.getId());
        if (totalSpent == null) totalSpent = BigDecimal.ZERO;
        BigDecimal currentOrderValue = order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount();
        if (currentOrderValue == null) currentOrderValue = BigDecimal.ZERO;

        LocalDateTime now = LocalDateTime.now();
        List<CustomerVoucherDto> awarded = new ArrayList<>();

        for (VoucherRule rule : activeRules) {
            Voucher rewardVoucher = rule.getVoucher();
            if (rewardVoucher == null || rewardVoucher.isDeleted() || !"ACTIVE".equalsIgnoreCase(rewardVoucher.getStatus())) {
                continue;
            }

            // Idempotency: don't award same rule twice for the same source order
            if (customerVoucherRepository.existsByCustomerIdAndRuleIdAndSourceOrderId(customer.getId(), rule.getId(), order.getId())) {
                continue;
            }

            // Max awards per customer cap
            long alreadyAwarded = customerVoucherRepository.countByCustomerIdAndRuleId(customer.getId(), rule.getId());
            if (alreadyAwarded >= rule.getMaxAwardsPerCustomer()) {
                continue;
            }

            boolean matched = false;
            switch (rule.getTriggerType().toUpperCase()) {
                case "ORDER_COUNT":
                    matched = BigDecimal.valueOf(completedCount).compareTo(rule.getThresholdValue()) >= 0;
                    break;
                case "TOTAL_SPENT":
                    matched = totalSpent.compareTo(rule.getThresholdValue()) >= 0;
                    break;
                case "SINGLE_ORDER_AMOUNT":
                    matched = currentOrderValue.compareTo(rule.getThresholdValue()) >= 0;
                    break;
                default:
                    break;
            }

            if (!matched) {
                continue;
            }

            String personalCode = generatePersonalCode(rewardVoucher.getCode());
            LocalDateTime expiresAt = now.plusDays(rule.getValidityDays() != null ? rule.getValidityDays() : 30);

            CustomerVoucher cv = CustomerVoucher.builder()
                    .customer(customer)
                    .voucher(rewardVoucher)
                    .rule(rule)
                    .sourceOrder(order)
                    .personalCode(personalCode)
                    .status("AVAILABLE")
                    .issuedAt(now)
                    .expiresAt(expiresAt)
                    .awardEmailSent(false)
                    .createdBy("AUTO_RULE")
                    .updatedBy("AUTO_RULE")
                    .build();

            CustomerVoucher saved = customerVoucherRepository.save(cv);
            log.info("[VoucherRule] Tự động cấp voucher {} cho khách hàng {} (Rule: {})",
                    personalCode, customer.getFullName(), rule.getRuleName());

            // Dispatch async award notification email
            voucherEmailSender.sendVoucherAwardEmailAsync(saved.getId());
            awarded.add(buildAvailableCard(rewardVoucher, saved, BigDecimal.ZERO, now));
        }

        return awarded;
    }

    // =========================================================================
    // 7. SCHEDULED EXPIRY SCANNER & REMINDER EMAIL DISPATCH
    // =========================================================================

    @Override
    @Transactional
    public Map<String, Object> scanAndSendExpiryReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // 1. Mark overdue AVAILABLE vouchers as EXPIRED
        List<CustomerVoucher> overdue = customerVoucherRepository.findExpiredStillAvailable(now);
        for (CustomerVoucher cv : overdue) {
            cv.setStatus("EXPIRED");
            cv.setUpdatedBy("SCHEDULER");
        }
        if (!overdue.isEmpty()) {
            customerVoucherRepository.saveAll(overdue);
        }

        // 2. Scan AVAILABLE vouchers expiring within next 3 days
        List<CustomerVoucher> expiring = customerVoucherRepository.findExpiringAvailableVouchers(now, now.plusDays(3).plusHours(1));
        int sent3d = 0;
        int sent1d = 0;
        int checked = expiring.size();

        for (CustomerVoucher cv : expiring) {
            long hoursLeft = ChronoUnit.HOURS.between(now, cv.getExpiresAt());
            int daysLeft = (int) Math.max(1, Math.ceil(hoursLeft / 24.0));

            if (daysLeft <= 1) {
                if (!today.equals(cv.getExpiryReminded1dDate())) {
                    if (voucherEmailSender.sendExpiryReminderEmail(cv, 1)) {
                        sent1d++;
                    }
                }
            } else if (daysLeft <= 3) {
                if (cv.getExpiryReminded3dDate() == null && !today.equals(cv.getExpiryReminded3dDate())) {
                    if (voucherEmailSender.sendExpiryReminderEmail(cv, daysLeft)) {
                        sent3d++;
                    }
                }
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("scannedAt", now);
        summary.put("expiredMarked", overdue.size());
        summary.put("expiringCandidates", checked);
        summary.put("reminded3Days", sent3d);
        summary.put("reminded1Day", sent1d);
        log.info("[VoucherExpiryScanner] Summary: {}", summary);
        return summary;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Set<Long> resolveCustomerIds(Long customerId, String phone, String email) {
        Set<Long> ids = new LinkedHashSet<>();
        if (customerId != null) {
            ids.add(customerId);
        }
        String qPhone = trimOrNull(phone);
        String qEmail = trimOrNull(email);

        // Also inspect authenticated storefront user if present
        String username = SecurityUtils.getCurrentUsername();
        if (username != null && !"SYSTEM".equals(username) && !"anonymousUser".equals(username)) {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                if (qPhone == null && user.getPhone() != null && !user.getPhone().isBlank()) {
                    qPhone = user.getPhone().trim();
                }
                if (qEmail == null && user.getEmail() != null && !user.getEmail().isBlank()) {
                    qEmail = user.getEmail().trim();
                }
            }
        }

        if (qPhone != null) {
            customerRepository.findAllByPhone(qPhone).forEach(c -> ids.add(c.getId()));
        }
        if (qEmail != null) {
            customerRepository.findAllByEmailIgnoreCase(qEmail).forEach(c -> ids.add(c.getId()));
        }
        return ids;
    }

    private boolean isChannelAllowed(String voucherChannel, String currentChannel) {
        if (voucherChannel == null || "ALL".equalsIgnoreCase(voucherChannel)) return true;
        return voucherChannel.equalsIgnoreCase(currentChannel);
    }

    private BigDecimal calculateDiscount(Voucher v, BigDecimal orderAmount) {
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal raw;
        if ("PERCENT".equalsIgnoreCase(v.getDiscountType())) {
            raw = orderAmount.multiply(v.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (v.getMaxDiscountAmount() != null && v.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                raw = raw.min(v.getMaxDiscountAmount());
            }
        } else {
            raw = v.getDiscountValue().setScale(0, RoundingMode.HALF_UP);
        }
        return raw.min(orderAmount).max(BigDecimal.ZERO);
    }

    private String generatePersonalCode(String baseCode) {
        String prefix = baseCode.length() > 12 ? baseCode.substring(0, 12) : baseCode;
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        String candidate;
        do {
            StringBuilder sb = new StringBuilder(prefix).append("-");
            for (int i = 0; i < 4; i++) {
                sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
            }
            candidate = sb.toString();
        } while (customerVoucherRepository.existsByPersonalCodeIgnoreCase(candidate));
        return candidate;
    }

    private VoucherValidationResponse invalid(String message, BigDecimal orderAmount, BigDecimal missingAmount) {
        return VoucherValidationResponse.builder()
                .valid(false)
                .message(message)
                .orderAmount(orderAmount)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(orderAmount)
                .missingAmount(missingAmount != null ? missingAmount : BigDecimal.ZERO)
                .build();
    }

    private VoucherDto toVoucherDto(Voucher v) {
        LocalDateTime now = LocalDateTime.now();
        String eff = v.getStatus();
        if ("ACTIVE".equalsIgnoreCase(eff)) {
            if (v.getEndDate() != null && now.isAfter(v.getEndDate())) {
                eff = "EXPIRED";
            } else if (v.getStartDate() != null && now.isBefore(v.getStartDate())) {
                eff = "UPCOMING";
            } else if (v.getTotalQuantity() != null && v.getUsedCount() >= v.getTotalQuantity()) {
                eff = "DEPLETED";
            }
        }
        Integer remaining = v.getTotalQuantity() != null
                ? Math.max(0, v.getTotalQuantity() - (v.getUsedCount() != null ? v.getUsedCount() : 0))
                : null;
        return VoucherDto.builder()
                .id(v.getId())
                .code(v.getCode())
                .name(v.getName())
                .description(v.getDescription())
                .discountType(v.getDiscountType())
                .discountValue(v.getDiscountValue())
                .maxDiscountAmount(v.getMaxDiscountAmount())
                .minOrderAmount(v.getMinOrderAmount())
                .totalQuantity(v.getTotalQuantity())
                .usedCount(v.getUsedCount())
                .remainingQuantity(remaining)
                .perCustomerLimit(v.getPerCustomerLimit())
                .channel(v.getChannel())
                .voucherType(v.getVoucherType())
                .startDate(v.getStartDate())
                .endDate(v.getEndDate())
                .status(v.getStatus())
                .effectiveStatus(eff)
                .createdAt(v.getCreatedAt())
                .build();
    }

    private VoucherRuleDto toRuleDto(VoucherRule r) {
        Voucher v = r.getVoucher();
        String summary = "";
        if (v != null) {
            summary = "PERCENT".equalsIgnoreCase(v.getDiscountType())
                    ? ("Giảm " + v.getDiscountValue().stripTrailingZeros().toPlainString() + "%"
                    + (v.getMaxDiscountAmount() != null ? " (tối đa " + formatVnd(v.getMaxDiscountAmount()) + ")" : ""))
                    : ("Giảm " + formatVnd(v.getDiscountValue()));
        }
        return VoucherRuleDto.builder()
                .id(r.getId())
                .ruleName(r.getRuleName())
                .description(r.getDescription())
                .triggerType(r.getTriggerType())
                .thresholdValue(r.getThresholdValue())
                .voucherId(v != null ? v.getId() : null)
                .voucherCode(v != null ? v.getCode() : null)
                .voucherName(v != null ? v.getName() : null)
                .voucherDiscountSummary(summary)
                .maxAwardsPerCustomer(r.getMaxAwardsPerCustomer())
                .validityDays(r.getValidityDays())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private VoucherUsageDto toUsageDto(VoucherUsage u) {
        return VoucherUsageDto.builder()
                .id(u.getId())
                .voucherId(u.getVoucher() != null ? u.getVoucher().getId() : null)
                .voucherCode(u.getVoucherCode())
                .voucherName(u.getVoucher() != null ? u.getVoucher().getName() : null)
                .orderId(u.getSalesOrder() != null ? u.getSalesOrder().getId() : null)
                .orderCode(u.getSalesOrder() != null ? u.getSalesOrder().getOrderCode() : null)
                .customerId(u.getCustomer() != null ? u.getCustomer().getId() : null)
                .customerName(u.getCustomer() != null ? u.getCustomer().getFullName() : "Khách lẻ tại quầy")
                .customerPhone(u.getCustomer() != null ? u.getCustomer().getPhone() : "---")
                .channel(u.getChannel())
                .orderSubtotal(u.getOrderSubtotal())
                .discountAmount(u.getDiscountAmount())
                .status(u.getStatus())
                .usedAt(u.getUsedAt())
                .refundedAt(u.getRefundedAt())
                .note(u.getNote())
                .build();
    }

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0₫";
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(amount) + "₫";
    }

    private String trimOrNull(String s) {
        return (s != null && !s.isBlank()) ? s.trim() : null;
    }

    private String upperOrNull(String s) {
        return (s != null && !s.isBlank() && !"ALL".equalsIgnoreCase(s.trim())) ? s.trim().toUpperCase() : null;
    }
}
