package com.sneakerstore.controller;

import com.sneakerstore.common.response.ApiResponse;
import com.sneakerstore.common.response.PageResponse;
import com.sneakerstore.dto.response.SalesOrderDto;
import com.sneakerstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<PageResponse<SalesOrderDto>> getOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(orderService.searchOrders(
                keyword, status, paymentMethod, channel, fromDate, toDate,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))));
    }

    @GetMapping("/status-counts")
    public ApiResponse<Map<String, Long>> getStatusCounts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.ok(orderService.getStatusCounts(
                keyword, paymentMethod, channel, fromDate, toDate));
    }

    @GetMapping("/{id}")
    public ApiResponse<SalesOrderDto> getById(@PathVariable Long id) {
        return ApiResponse.ok(orderService.getById(id));
    }

    @GetMapping("/by-customer/{customerId}")
    public ApiResponse<List<SalesOrderDto>> getByCustomer(@PathVariable Long customerId) {
        return ApiResponse.ok(orderService.getOrdersByCustomer(customerId));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<SalesOrderDto> updateStatus(
            @PathVariable Long id, @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        String note = payload.get("note");
        return ApiResponse.ok("Cập nhật trạng thái đơn hàng thành công",
                orderService.updateStatus(id, status, note));
    }

    @PatchMapping("/{id}/note")
    public ApiResponse<SalesOrderDto> updateNote(
            @PathVariable Long id, @RequestBody Map<String, String> payload) {
        return ApiResponse.ok("Cập nhật ghi chú đơn hàng thành công",
                orderService.updateNote(id, payload.get("note")));
    }

    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        byte[] bytes = orderService.exportOrdersExcel(keyword, status, paymentMethod, channel, fromDate, toDate);
        String filename = "ADHA_DanhSachDonHang_" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}

