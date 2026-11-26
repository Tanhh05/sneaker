package com.sneakerstore.controller;

import com.sneakerstore.common.response.ApiResponse;
import com.sneakerstore.common.response.PageResponse;
import com.sneakerstore.dto.request.VoucherRequest;
import com.sneakerstore.dto.request.VoucherRuleRequest;
import com.sneakerstore.dto.request.VoucherValidateRequest;
import com.sneakerstore.dto.response.*;
import com.sneakerstore.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping
    public ApiResponse<PageResponse<VoucherDto>> searchVouchers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String discountType,
            @RequestParam(required = false) String voucherType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(voucherService.searchVouchers(
                keyword, channel, discountType, voucherType, status, fromDate, toDate,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))));
    }

    @GetMapping("/all")
    public ApiResponse<List<VoucherDto>> getAllVouchers() {
        return ApiResponse.ok(voucherService.getAllVouchers());
    }

    @GetMapping("/{id}")
    public ApiResponse<VoucherDto> getById(@PathVariable Long id) {
        return ApiResponse.ok(voucherService.getVoucherById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<VoucherDto> createVoucher(@Valid @RequestBody VoucherRequest request) {
        return ApiResponse.ok("Tạo voucher mới thành công", voucherService.createVoucher(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<VoucherDto> updateVoucher(@PathVariable Long id, @Valid @RequestBody VoucherRequest request) {
        return ApiResponse.ok("Cập nhật voucher thành công", voucherService.updateVoucher(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<VoucherDto> toggleStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        return ApiResponse.ok("Cập nhật trạng thái voucher thành công",
                voucherService.toggleVoucherStatus(id, payload.get("status")));
    }

    @DeleteMapping("/{id}/soft-delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> softDeleteVoucher(@PathVariable Long id) {
        voucherService.softDeleteVoucher(id);
        return ApiResponse.ok("Đã xóa mềm voucher", null);
    }

    // ==================== DYNAMIC TRIGGER RULES ====================

    @GetMapping("/rules")
    public ApiResponse<List<VoucherRuleDto>> getRules() {
        return ApiResponse.ok(voucherService.getAllRules());
    }

    @PostMapping("/rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<VoucherRuleDto> createRule(@Valid @RequestBody VoucherRuleRequest request) {
        return ApiResponse.ok("Tạo quy tắc thưởng động thành công", voucherService.createRule(request));
    }

    @PutMapping("/rules/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<VoucherRuleDto> updateRule(@PathVariable Long id, @Valid @RequestBody VoucherRuleRequest request) {
        return ApiResponse.ok("Cập nhật quy tắc thưởng thành công", voucherService.updateRule(id, request));
    }

    @PatchMapping("/rules/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<VoucherRuleDto> toggleRuleStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        return ApiResponse.ok("Cập nhật trạng thái quy tắc thành công",
                voucherService.toggleRuleStatus(id, payload.get("status")));
    }

    @DeleteMapping("/rules/{id}/soft-delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> softDeleteRule(@PathVariable Long id) {
        voucherService.softDeleteRule(id);
        return ApiResponse.ok("Đã xóa mềm quy tắc thưởng", null);
    }

    // ==================== USAGE LOGS & POS WALLET ====================

    @GetMapping("/usages")
    public ApiResponse<PageResponse<VoucherUsageDto>> searchUsages(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(voucherService.searchUsages(
                keyword, channel, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "usedAt"))));
    }

    @GetMapping("/pos/available")
    public ApiResponse<List<CustomerVoucherDto>> getPosAvailableVouchers(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false, defaultValue = "0") BigDecimal orderAmount) {
        return ApiResponse.ok(voucherService.getAvailableVouchers("POS", customerId, phone, null, orderAmount));
    }

    @PostMapping("/validate")
    public ApiResponse<VoucherValidationResponse> validateVoucher(@RequestBody VoucherValidateRequest request) {
        return ApiResponse.ok(voucherService.validateVoucher(request));
    }

    @PostMapping("/scan-expiring")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Map<String, Object>> triggerExpiryScan() {
        return ApiResponse.ok("Đã chạy quét voucher sắp hết hạn", voucherService.scanAndSendExpiryReminders());
    }
}
