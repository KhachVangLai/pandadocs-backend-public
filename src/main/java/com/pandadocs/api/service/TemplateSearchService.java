package com.pandadocs.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.pandadocs.api.dto.TemplateCardDTO;
import com.pandadocs.api.model.Template;
import com.pandadocs.api.model.TemplateStatus;
import com.pandadocs.api.repository.CategoryRepository;
import com.pandadocs.api.repository.TemplateRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for searching templates based on various criteria
 * Used by ChatService to find templates for AI recommendations
 */
@Service
@Slf4j
public class TemplateSearchService {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Searches for templates based on keyword, category, and price range
     *
     * @param keyword   Search keyword (title, description)
     * @param category  Category name (optional)
     * @param maxPrice  Maximum price in VND (optional)
     * @param limit     Maximum number of results (default 5)
     * @return List of TemplateCardDTO
     */
    public List<TemplateCardDTO> searchTemplates(
            String keyword,
            String category,
            Double maxPrice,
            Integer limit) {

        log.debug("Searching templates: keyword={}, category={}, maxPrice={}, limit={}",
                keyword, category, maxPrice, limit);

        List<Template> allTemplates = templateRepository.findByStatus(TemplateStatus.PUBLISHED);

        if (keyword != null && !keyword.isBlank()) {
            String lowerKeyword = keyword.toLowerCase();
            allTemplates = allTemplates.stream()
                    .filter(t -> (t.getTitle() != null && t.getTitle().toLowerCase().contains(lowerKeyword)) ||
                                 (t.getDescription() != null && t.getDescription().toLowerCase().contains(lowerKeyword)))
                    .collect(Collectors.toList());
        }

        if (category != null && !category.isBlank()) {
            String lowerCategory = category.toLowerCase();
            allTemplates = allTemplates.stream()
                    .filter(t -> t.getCategory() != null &&
                                 t.getCategory().getName().toLowerCase().contains(lowerCategory))
                    .collect(Collectors.toList());
        }

        if (maxPrice != null && maxPrice >= 0) {
            allTemplates = allTemplates.stream()
                    .filter(t -> t.getPrice() != null && t.getPrice() <= maxPrice)
                    .collect(Collectors.toList());
        }

        allTemplates.sort((t1, t2) -> {
            int downloadCompare = Integer.compare(t2.getDownloads(), t1.getDownloads());
            if (downloadCompare != 0) return downloadCompare;

            return Float.compare(t2.getRating(), t1.getRating());
        });

        int resultLimit = (limit != null && limit > 0) ? Math.min(limit, 10) : 5;
        allTemplates = allTemplates.stream()
                .limit(resultLimit)
                .collect(Collectors.toList());

        log.info("Found {} templates matching criteria", allTemplates.size());

        return allTemplates.stream()
                .map(this::convertToTemplateCardDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets popular templates (top by downloads)
     *
     * @param limit Maximum number of results
     * @return List of TemplateCardDTO
     */
    public List<TemplateCardDTO> getPopularTemplates(int limit) {
        List<Template> templates = templateRepository.findTop10ByStatusOrderByDownloadsDesc(TemplateStatus.PUBLISHED);

        return templates.stream()
                .limit(Math.min(limit, 10))
                .map(this::convertToTemplateCardDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets template details by ID
     *
     * @param templateId Template ID
     * @return TemplateCardDTO or null if not found
     */
    public TemplateCardDTO getTemplateById(Long templateId) {
        return templateRepository.findByIdWithCategoryAndAuthor(templateId)
                .filter(t -> t.getStatus() == TemplateStatus.PUBLISHED)
                .map(this::convertToTemplateCardDTO)
                .orElse(null);
    }

    /**
     * Gets free templates
     *
     * @param limit Maximum number of results
     * @return List of TemplateCardDTO
     */
    public List<TemplateCardDTO> getFreeTemplates(int limit) {
        List<Template> templates = templateRepository.findByStatus(TemplateStatus.PUBLISHED);

        return templates.stream()
                .filter(t -> t.getPrice() != null && t.getPrice() == 0.0)
                .limit(Math.min(limit, 10))
                .map(this::convertToTemplateCardDTO)
                .collect(Collectors.toList());
    }

    /**
     * Converts Template entity to TemplateCardDTO
     *
     * @param template Template entity
     * @return TemplateCardDTO
     */
    private TemplateCardDTO convertToTemplateCardDTO(Template template) {
        String previewImage = null;
        if (template.getPreviewImages() != null && !template.getPreviewImages().isEmpty()) {
            previewImage = template.getPreviewImages().get(0);
        }

        return TemplateCardDTO.builder()
                .id(template.getId())
                .title(template.getTitle())
                .description(template.getDescription())
                .price(template.getPrice())
                .previewImage(previewImage)
                .category(template.getCategory() != null ? template.getCategory().getName() : null)
                .rating(template.getRating())
                .downloads(template.getDownloads())
                .build();
    }
}
