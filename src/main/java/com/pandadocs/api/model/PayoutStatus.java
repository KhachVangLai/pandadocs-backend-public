package com.pandadocs.api.model;

public enum PayoutStatus {
    PENDING,    // Waiting for the admin to send payment.
    PAID,       // Seller has been paid.
    REJECTED    // Payout request was rejected.
}
