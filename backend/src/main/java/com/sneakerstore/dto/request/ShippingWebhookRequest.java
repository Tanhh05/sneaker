package com.sneakerstore.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingWebhookRequest {
    private String orderCode;
    private String deliveryStatus; // DELIVERED, COMPLETED, FAILED, RETURNED, CANCELLED
    private String trackingCode;
    private String carrierName;
    private String note;
}
