package com.pandadocs.api.model;

public enum TemplateStatus {
    PENDING_REVIEW, // Waiting for review.
    APPROVED,       // Approved but not yet published.
    PUBLISHED,      // Published for sale.
    REJECTED        // Rejected during review.
}
