package com.pandadocs.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSellerProfileRequest {
    @NotBlank(message = "Business name is required")
    private String businessName;

    private String description;

    // Bank information (required for receiving payments)
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountHolderName;
}
