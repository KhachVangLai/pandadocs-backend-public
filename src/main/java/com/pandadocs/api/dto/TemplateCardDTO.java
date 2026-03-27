package com.pandadocs.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateCardDTO {
    private Long id;
    private String title;
    private String description;
    private Double price;
    private String previewImage;
    private String category;
    private Float rating;
    private Integer downloads;
}
