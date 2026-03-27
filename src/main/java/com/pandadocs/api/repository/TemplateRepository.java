package com.pandadocs.api.repository;

import com.pandadocs.api.model.Template;
import com.pandadocs.api.model.TemplateStatus;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {

    Page<Template> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    @Query("SELECT t FROM Template t LEFT JOIN FETCH t.category LEFT JOIN FETCH t.author LEFT JOIN FETCH t.previewImages WHERE t.status = :status")
    List<Template> findByStatus(@Param("status") TemplateStatus status);

    long countByAuthorIdAndStatus(Long authorId, TemplateStatus status);

    long countByStatus(TemplateStatus status);

    @Query(value = "SELECT t FROM Template t LEFT JOIN FETCH t.category LEFT JOIN FETCH t.author LEFT JOIN FETCH t.previewImages WHERE t.status = :status",
           countQuery = "SELECT COUNT(t) FROM Template t WHERE t.status = :status")
    Page<Template> findByStatus(@Param("status") TemplateStatus status, Pageable pageable);

    @Query(value = "SELECT t FROM Template t LEFT JOIN FETCH t.category LEFT JOIN FETCH t.author LEFT JOIN FETCH t.previewImages",
           countQuery = "SELECT COUNT(t) FROM Template t")
    Page<Template> findAllWithCategoryAndAuthor(Pageable pageable);

    @Query("SELECT t FROM Template t LEFT JOIN FETCH t.category LEFT JOIN FETCH t.author LEFT JOIN FETCH t.previewImages WHERE t.id = :id")
    java.util.Optional<Template> findByIdWithCategoryAndAuthor(@Param("id") Long id);

    @Query("SELECT t FROM Template t LEFT JOIN FETCH t.category LEFT JOIN FETCH t.author LEFT JOIN FETCH t.previewImages WHERE t.status = :status ORDER BY t.downloads DESC")
    List<Template> findTop10ByStatusOrderByDownloadsDesc(@Param("status") TemplateStatus status);
}
