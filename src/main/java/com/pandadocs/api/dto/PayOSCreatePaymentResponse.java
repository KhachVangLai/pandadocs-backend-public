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
        private String bin;           // Mã BIN ngân hàng
        private String accountNumber; // Số tài khoản
        private String accountName;   // Tên tài khoản
        private Integer amount;       // Số tiền
        private String description;   // Mô tả
        private Long orderCode;       // Mã đơn hàng
        private String currency;      // Loại tiền tệ (VND)
        private String paymentLinkId; // ID payment link
        private String status;        // Trạng thái: PENDING, PAID, CANCELLED
        private String checkoutUrl;   // URL để user thanh toán
        private String qrCode;        // QR code (base64)
    }
}
