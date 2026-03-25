package com.pandadocs.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDashboardDTO {
    private double monthlyRevenue;  // Doanh thu trong 30 ngày qua
    private double rewardCosts;     // Chi phí trả cho seller trong 30 ngày qua
    private long totalTemplates;    // Tổng số template trên hệ thống
    private long totalDownloads;    // Tổng số lượt tải trong 30 ngày qua
    private long dailyUsers;        // Số user mới trong 24 giờ qua

    // Template statistics by status
    private long pendingTemplates;   // Số template đang chờ duyệt (PENDING_REVIEW)
    private long approvedTemplates;  // Số template đã duyệt nhưng chưa đăng (APPROVED)
    private long publishedTemplates; // Số template đã xuất bản (PUBLISHED)
    private long rejectedTemplates;  // Số template bị từ chối (REJECTED)
}