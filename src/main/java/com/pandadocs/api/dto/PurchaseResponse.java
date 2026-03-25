package com.pandadocs.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResponse {
    private Long orderId;
    private String paymentUrl;
    private String message;
}
