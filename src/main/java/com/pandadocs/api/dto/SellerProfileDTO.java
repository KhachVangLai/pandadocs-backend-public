package com.pandadocs.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerProfileDTO {
    private Long userId;
    private String businessName;
    private String description;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountHolderName;
    private boolean hasBankInfo;
}
