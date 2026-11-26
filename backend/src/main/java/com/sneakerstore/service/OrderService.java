package com.sneakerstore.service;

import com.sneakerstore.common.response.PageResponse;
import com.sneakerstore.dto.request.SalesOrderRequest;
import com.sneakerstore.dto.response.SalesOrderDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface OrderService {

    PageResponse<SalesOrderDto> searchOrders(
            String keyword, String status, String paymentMethod, String channel,
            LocalDate fromDate, LocalDate toDate, Pageable pageable);

    Map<String, Long> getStatusCounts(
            String keyword, String paymentMethod, String channel,
            LocalDate fromDate, LocalDate toDate);

    List<SalesOrderDto> getOrdersByCustomer(Long customerId);

    SalesOrderDto getById(Long id);

    SalesOrderDto createOrder(SalesOrderRequest request);

    SalesOrderDto updateStatus(Long id, String targetStatus, String note);

    SalesOrderDto updateNote(Long id, String note);

    byte[] exportOrdersExcel(
            String keyword, String status, String paymentMethod, String channel,
            LocalDate fromDate, LocalDate toDate);
}
