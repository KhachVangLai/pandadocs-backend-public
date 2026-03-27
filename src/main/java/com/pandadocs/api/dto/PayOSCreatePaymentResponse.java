package com.pandadocs.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayOSCreatePaymentResponse {
    private String code;              // Response code (00 = success)
    private String desc;              // Description
    private PayOSPaymentData data;    // Payment data

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayOSPaymentData {
        private String bin;           // Bank BIN.
        private String accountNumber; // Destination account number.
        private String accountName;   // Destination account name.
        private Integer amount;       // Payment amount.
        private String description;   // Payment description.
        private Long orderCode;       // Order code.
        private String currency;      // Currency code, typically VND.
        private String paymentLinkId; // Payment link ID.
        private String status;        // Payment status: PENDING, PAID, or CANCELLED.
        private String checkoutUrl;   // URL where the user completes payment.
        private String qrCode;        // QR code (base64)
    }
}
