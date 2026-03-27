package com.pandadocs.api.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class CollectionDTO {
    private Long id;
    private String name;
    private String description;
    private int templateCount;
    private List<TemplateDTO> templates; // Optional; typically returned only for detail views.

    public List<TemplateDTO> getTemplates() {
        return templates;
    }
    
    public void setTemplates(List<TemplateDTO> templates) {
        this.templates = templates;
    }
}
