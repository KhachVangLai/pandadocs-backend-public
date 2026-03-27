package com.pandadocs.api.dto;

import com.pandadocs.api.model.TemplateStatus;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class TemplateDTO {
    private Long id;
    private String title;
    private String description;
    private Double price;
    private String fileUrl;
    private TemplateStatus status;
    private CategoryDTO category;
    private AuthorDTO author;
    private float rating;
    private int reviewCount;
    private List<String> previewImages;
}
