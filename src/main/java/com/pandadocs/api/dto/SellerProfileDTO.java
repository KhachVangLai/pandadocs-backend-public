package com.pandadocs.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerProfileDTO {
    private Long userId;
    private String businessName;
    private String description;

    // Bank information
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountHolderName;

    // Indicator if bank info is complete
    private boolean hasBankInfo;
}
