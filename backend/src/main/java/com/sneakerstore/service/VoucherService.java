package com.sneakerstore.service;

import com.sneakerstore.common.response.PageResponse;
import com.sneakerstore.dto.request.VoucherRequest;
import com.sneakerstore.dto.request.VoucherRuleRequest;
import com.sneakerstore.dto.request.VoucherValidateRequest;
import com.sneakerstore.dto.response.*;
import com.sneakerstore.entity.Customer;
import com.sneakerstore.entity.SalesOrder;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface VoucherService {

    // Master Data CRUD
    PageResponse<VoucherDto> searchVouchers(String keyword, String channel, String discountType,
                                            String voucherType, String status,
                                            LocalDate fromDate, LocalDate toDate, Pageable pageable);

    List<VoucherDto> getAllVouchers();

    VoucherDto getVoucherById(Long id);

    VoucherDto createVoucher(VoucherRequest request);

    VoucherDto updateVoucher(Long id, VoucherRequest request);

    VoucherDto toggleVoucherStatus(Long id, String status);

    void softDeleteVoucher(Long id);

    // Dynamic Trigger Rules CRUD
    List<VoucherRuleDto> getAllRules();

    VoucherRuleDto createRule(VoucherRuleRequest request);

    VoucherRuleDto updateRule(Long id, VoucherRuleRequest request);

    VoucherRuleDto toggleRuleStatus(Long id, String status);

    void softDeleteRule(Long id);

    // Usage History & Personal Wallet
    PageResponse<VoucherUsageDto> searchUsages(String keyword, String channel, String status, Pageable pageable);

    List<CustomerVoucherDto> getAvailableVouchers(String channel, Long customerId, String phone, String email, BigDecimal orderAmount);

    VoucherValidationResponse validateVoucher(VoucherValidateRequest request);

    // Order Lifecycle Hooks
    BigDecimal applyVoucherToOrder(SalesOrder order, String voucherCode, String channel, Customer customer, BigDecimal orderSubtotal);

    void refundVoucherForCancelledOrder(Long salesOrderId, String reason);

    List<CustomerVoucherDto> evaluateAndAwardRulesForCompletedOrder(Long salesOrderId);

    // Scheduled Expiry Scanner
    Map<String, Object> scanAndSendExpiryReminders();
}
