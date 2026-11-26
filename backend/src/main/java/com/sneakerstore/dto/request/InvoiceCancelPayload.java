package com.sneakerstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCancelPayload {
    @NotBlank(message = "Bắt buộc nhập lý do hủy hóa đơn")
    private String reason;
}
