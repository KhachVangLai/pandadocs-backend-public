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
    /**
     * Template ID
     */
    private Long id;

    /**
     * Template title
     */
    private String title;

    /**
     * Template description
     */
    private String description;

    /**
     * Price in VND (0 for free templates)
     */
    private Double price;

    /**
     * Preview image URL
     */
    private String previewImage;

    /**
     * Category name
     */
    private String category;

    /**
     * Rating (0-5)
     */
    private Float rating;

    /**
     * Number of downloads
     */
    private Integer downloads;
}
