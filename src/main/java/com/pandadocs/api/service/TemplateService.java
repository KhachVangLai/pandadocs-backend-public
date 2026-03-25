package com.pandadocs.api.service;

import com.pandadocs.api.dto.AuthorDTO;
import com.pandadocs.api.dto.CategoryDTO;
import com.pandadocs.api.dto.TemplateDTO;
import com.pandadocs.api.model.Template;
import org.springframework.stereotype.Service;
import com.pandadocs.api.model.User;
import com.pandadocs.api.model.Category;

@Service
public class TemplateService {

    public TemplateDTO convertToDto(Template template) {
        TemplateDTO templateDTO = new TemplateDTO();
        templateDTO.setId(template.getId());
        templateDTO.setTitle(template.getTitle());
        templateDTO.setDescription(template.getDescription());
        templateDTO.setPrice(template.getPrice());
        templateDTO.setFileUrl(template.getFileUrl());
        templateDTO.setStatus(template.getStatus());  // Map status
        templateDTO.setRating(template.getRating()); // Map rating
        templateDTO.setReviewCount(template.getReviewCount()); // Map reviewCount
        templateDTO.setPreviewImages(template.getPreviewImages()); // Map preview images

        if (template.getCategory() != null) {
            CategoryDTO categoryDTO = new CategoryDTO();
            categoryDTO.setId(template.getCategory().getId());
            categoryDTO.setName(template.getCategory().getName());
            templateDTO.setCategory(categoryDTO);
        }

        if (template.getAuthor() != null) {
            AuthorDTO authorDTO = new AuthorDTO();
            authorDTO.setId(template.getAuthor().getId());
            authorDTO.setUsername(template.getAuthor().getUsername());
            templateDTO.setAuthor(authorDTO);
        }

        return templateDTO;
    }

    public Template convertToEntity(TemplateDTO templateDTO, User author, Category category) {
        Template template = new Template();
        template.setTitle(templateDTO.getTitle());
        template.setDescription(templateDTO.getDescription());
        template.setPrice(templateDTO.getPrice());
        template.setFileUrl(templateDTO.getFileUrl());
        template.setAuthor(author);
        template.setCategory(category);
        return template;
    }
}