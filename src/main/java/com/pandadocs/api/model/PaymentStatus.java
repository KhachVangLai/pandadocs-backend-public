package com.pandadocs.api.model;

public enum PaymentStatus {
    PENDING_PAYMENT,    // Waiting for the user to pay.
    PAID,               // Payment completed successfully.
    FAILED,             // Payment failed.
    CANCELLED           // Payment was canceled by the user.
}
