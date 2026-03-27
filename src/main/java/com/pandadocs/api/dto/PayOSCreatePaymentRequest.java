package com.pandadocs.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayOSCreatePaymentRequest {
    private Long orderCode;          // Unique order code.
    private Integer amount;          // Payment amount in VND.
    private String description;      // Order description.
    private String buyerName;        // Optional buyer name.
    private String buyerEmail;       // Optional buyer email.
    private String buyerPhone;       // Optional buyer phone number.
    private String buyerAddress;     // Optional buyer address.
    private String returnUrl;        // Redirect URL after successful payment.
    private String cancelUrl;        // Redirect URL after the user cancels payment.
    private List<Item> items;        // Line items included in the payment.
    private String signature;        // Signature used to verify the request.

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String name;
        private Integer quantity;
        private Integer price;
    }
}
