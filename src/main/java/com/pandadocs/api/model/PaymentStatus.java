package com.pandadocs.api.model;

public enum PaymentStatus {
    PENDING_PAYMENT,    // Đang chờ user thanh toán
    PAID,               // Đã thanh toán thành công
    FAILED,             // Thanh toán thất bại
    CANCELLED           // User hủy thanh toán
}
