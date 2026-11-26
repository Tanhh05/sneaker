package com.sneakerstore.controller;

import com.sneakerstore.common.response.ApiResponse;
import com.sneakerstore.common.response.PageResponse;
import com.sneakerstore.dto.request.InvoiceAdjustRequest;
import com.sneakerstore.dto.request.InvoiceCancelPayload;
import com.sneakerstore.dto.request.InvoiceIssueRequest;
import com.sneakerstore.dto.request.InvoiceSendEmailRequest;
import com.sneakerstore.dto.response.InvoiceDto;
import com.sneakerstore.dto.response.InvoiceKpiDto;
import com.sneakerstore.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ApiResponse<PageResponse<InvoiceDto>> getInvoices(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String saleChannel,
            @RequestParam(required = false) String invoiceType,
            @RequestParam(required = false) String invoiceStatus,
            @RequestParam(required = false) String emailStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(invoiceService.searchInvoices(
                keyword, saleChannel, invoiceType, invoiceStatus, emailStatus, fromDate, toDate,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "issuedAt", "id"))));
    }

    @GetMapping("/vat-config")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ApiResponse<Map<String, Object>> getVatConfig() {
        return ApiResponse.ok(invoiceService.getVatConfig());
    }

    @GetMapping("/kpi")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ApiResponse<InvoiceKpiDto> getKpi(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.ok(invoiceService.getKpi(fromDate, toDate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ApiResponse<InvoiceDto> getById(@PathVariable Long id) {
        return ApiResponse.ok(invoiceService.getById(id));
    }

    @PostMapping("/issue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ApiResponse<InvoiceDto> issueInvoice(@Valid @RequestBody InvoiceIssueRequest request) {
        return ApiResponse.ok("Phát hành hóa đơn thành công", invoiceService.issueManualInvoice(request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<InvoiceDto> cancelDirect(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceCancelPayload payload) {
        return ApiResponse.ok("Đã hủy hóa đơn thành công", invoiceService.cancelInvoiceDirect(id, payload.getReason()));
    }

    @PostMapping("/{id}/cancel-request")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ApiResponse<InvoiceDto> requestCancel(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceCancelPayload payload) {
        return ApiResponse.ok("Đã gửi yêu cầu hủy hóa đơn chờ duyệt",
                invoiceService.requestCancelInvoice(id, payload.getReason()));
    }

    @PostMapping("/cancel-requests/{requestId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<InvoiceDto> approveCancel(
            @PathVariable Long requestId,
            @RequestBody(required = false) Map<String, String> payload) {
        String note = payload != null ? payload.get("reviewNote") : null;
        return ApiResponse.ok("Đã phê duyệt hủy hóa đơn", invoiceService.approveCancelRequest(requestId, note));
    }

    @PostMapping("/cancel-requests/{requestId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<InvoiceDto> rejectCancel(
            @PathVariable Long requestId,
            @RequestBody(required = false) Map<String, String> payload) {
        String note = payload != null ? payload.get("reviewNote") : null;
        return ApiResponse.ok("Đã từ chối yêu cầu hủy hóa đơn", invoiceService.rejectCancelRequest(requestId, note));
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<InvoiceDto> adjustInvoice(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceAdjustRequest request) {
        return ApiResponse.ok("Đã lập hóa đơn điều chỉnh / thay thế thành công",
                invoiceService.adjustInvoice(id, request));
    }

    @PostMapping("/{id}/send-email")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ApiResponse<InvoiceDto> sendEmail(
            @PathVariable Long id,
            @RequestBody(required = false) InvoiceSendEmailRequest request) {
        String email = request != null ? request.getRecipientEmail() : null;
        return ApiResponse.ok("Đã tiếp nhận yêu cầu gửi hóa đơn qua Email",
                invoiceService.sendInvoiceEmail(id, email));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        InvoiceDto dto = invoiceService.getById(id);
        byte[] bytes = invoiceService.generateInvoicePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"HoaDon_" + dto.getInvoiceCode() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String saleChannel,
            @RequestParam(required = false) String invoiceType,
            @RequestParam(required = false) String invoiceStatus,
            @RequestParam(required = false) String emailStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        byte[] bytes = invoiceService.exportInvoicesExcel(
                keyword, saleChannel, invoiceType, invoiceStatus, emailStatus, fromDate, toDate);
        String filename = "ADHA_BangKeHoaDon_" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
