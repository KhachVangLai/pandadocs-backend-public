package com.pandadocs.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayOSWebhookData {
    private String code;              // Mã response (00 = success)
    private String desc;              // Mô tả
    private boolean success;          // true nếu thanh toán thành công
    private WebhookPaymentData data;  // Thông tin thanh toán
    private String signature;         // Chữ ký để verify webhook

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookPaymentData {
        private Long orderCode;           // Mã đơn hàng
        private Integer amount;           // Số tiền
        private String description;       // Mô tả
        private String accountNumber;     // Số tài khoản nhận
        private String reference;         // Mã tham chiếu giao dịch
        private String transactionDateTime; // Thời gian giao dịch
        private String currency;          // Loại tiền (VND)
        private String paymentLinkId;     // ID payment link
        private String code;              // Mã giao dịch
        private String desc;              // Mô tả giao dịch
        private String counterAccountBankId; // Mã ngân hàng của người chuyển
        private String counterAccountBankName; // Tên ngân hàng người chuyển
        private String counterAccountName;    // Tên tài khoản người chuyển
        private String counterAccountNumber;  // Số tài khoản người chuyển
        private String virtualAccountName;    // Tên tài khoản ảo
        private String virtualAccountNumber;  // Số tài khoản ảo
    }
}
