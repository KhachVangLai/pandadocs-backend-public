package com.pandadocs.api.model;

public enum PayoutStatus {
    PENDING,    // Chờ admin chuyển tiền
    PAID,       // Đã chuyển tiền cho seller
    REJECTED    // Admin từ chối trả tiền (reject template)
}
