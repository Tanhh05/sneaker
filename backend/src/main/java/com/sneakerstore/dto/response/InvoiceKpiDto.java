package com.sneakerstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceKpiDto {
    private long totalInvoices;
    private BigDecimal totalValidRevenue;
    private BigDecimal totalValidTax;
    private long posInvoices;
    private long onlineInvoices;
    private long cancelledInvoices;
    private long pendingCancelCount;
}
