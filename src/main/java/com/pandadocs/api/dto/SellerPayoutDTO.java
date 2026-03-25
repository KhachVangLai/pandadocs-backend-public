package com.pandadocs.api.dto;

import com.pandadocs.api.model.PayoutStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerPayoutDTO {
    private Long id;
    private Long templateId;
    private String templateTitle;
    private Long sellerId;
    private String sellerUsername;
    private String sellerBusinessName;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountHolderName;
    private Double proposedPrice;
    private Double agreedPrice;
    private PayoutStatus status;
    private String adminNote;
    private Instant paidAt;
    private Instant createdAt;
}
