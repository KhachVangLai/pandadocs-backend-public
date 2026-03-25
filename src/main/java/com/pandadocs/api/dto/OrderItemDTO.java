package com.pandadocs.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemDTO {
    private Long templateId;
    private String templateTitle;
    private Double price;
}