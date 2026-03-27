package com.pandadocs.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDashboardDTO {
    private double monthlyRevenue;  // Revenue from the last 30 days.
    private double rewardCosts;     // Seller payout costs from the last 30 days.
    private long totalTemplates;    // Total templates in the system.
    private long totalDownloads;    // Downloads from the last 30 days.
    private long dailyUsers;        // New users from the last 24 hours.

    // Template statistics by status
    private long pendingTemplates;   // Templates waiting for review.
    private long approvedTemplates;  // Templates approved but not yet published.
    private long publishedTemplates; // Templates currently published.
    private long rejectedTemplates;  // Templates rejected during review.
}
