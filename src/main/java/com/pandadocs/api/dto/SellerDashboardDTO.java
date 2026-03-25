package com.pandadocs.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerDashboardDTO {
    private long submittedCount;
    private long pendingReviewCount;
    private long approvedCount;
    private long rejectedCount;
    private double totalEarnings;
}