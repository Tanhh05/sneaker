package com.sneakerstore.service;

import com.sneakerstore.common.response.PageResponse;
import com.sneakerstore.dto.request.InvoiceAdjustRequest;
import com.sneakerstore.dto.request.InvoiceIssueRequest;
import com.sneakerstore.dto.request.ShippingWebhookRequest;
import com.sneakerstore.dto.response.InvoiceDto;
import com.sneakerstore.dto.response.InvoiceKpiDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface InvoiceService {

    PageResponse<InvoiceDto> searchInvoices(
            String keyword, String saleChannel, String invoiceType,
            String invoiceStatus, String emailStatus,
            LocalDate fromDate, LocalDate toDate, Pageable pageable);

    InvoiceKpiDto getKpi(LocalDate fromDate, LocalDate toDate);

    InvoiceDto getById(Long id);

    InvoiceDto autoIssueForCompletedOrder(Long orderId, InvoiceIssueRequest optionalConfig);

    InvoiceDto issueManualInvoice(InvoiceIssueRequest request);

    InvoiceDto cancelInvoiceDirect(Long id, String reason);

    InvoiceDto requestCancelInvoice(Long id, String reason);

    InvoiceDto approveCancelRequest(Long requestId, String reviewNote);

    InvoiceDto rejectCancelRequest(Long requestId, String reviewNote);

    InvoiceDto adjustInvoice(Long originalInvoiceId, InvoiceAdjustRequest request);

    InvoiceDto sendInvoiceEmail(Long id, String recipientEmail);

    byte[] generateInvoicePdf(Long id);

    byte[] exportInvoicesExcel(
            String keyword, String saleChannel, String invoiceType,
            String invoiceStatus, String emailStatus,
            LocalDate fromDate, LocalDate toDate);

    Map<String, Object> getVatConfig();

    Map<String, Object> processShippingWebhook(ShippingWebhookRequest request);
}
