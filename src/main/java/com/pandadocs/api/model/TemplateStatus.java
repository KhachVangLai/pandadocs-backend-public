package com.pandadocs.api.model;

public enum TemplateStatus {
    PENDING_REVIEW, // Chờ duyệt
    APPROVED,       // Đã duyệt (nhưng chưa đăng)
    PUBLISHED,      // Đã đăng bán
    REJECTED        // Bị từ chối
}