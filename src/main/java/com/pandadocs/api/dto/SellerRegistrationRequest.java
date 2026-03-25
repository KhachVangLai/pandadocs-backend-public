package com.pandadocs.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerRegistrationRequest {
    private String businessName;
    private String description;
}