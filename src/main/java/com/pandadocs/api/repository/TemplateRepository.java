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

    // Spring Data JPA sẽ tự tạo query tìm kiếm title chứa keyword (không phân biệt
    // hoa thường)
    Page<Template> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    // Eager load Category and Author for single status query
    @Query("SELECT t FROM Template t LEFT JOIN FETCH t.category LEFT JOIN FETCH t.author LEFT JOIN FETCH t.previewImages WHERE t.status = :status")
    List<Template> findByStatus(@Param("status") TemplateStatus status);

    // Đếm số template của một tác giả theo trạng thái
    long countByAuthorIdAndStatus(Long authorId, TemplateStatus status);

    // Đếm tất cả templates theo status (cho admin dashboard)
    long countByStatus(TemplateStatus status);

    // Tìm templates theo status với pagination (cho admin) - Eager load Category and Author
    @Query(value = "SELECT t FROM Template t LEFT JOIN FETCH t.category LEFT JOIN FETCH t.author LEFT JOIN FETCH t.previewImages WHERE t.status = :status",
           countQuery = "SELECT COUNT(t) FROM Template t WHERE t.status = :status")
    Page<Template> findByStatus(@Param("status") TemplateStatus status, Pageable pageable);

    // Eager load Category and Author for findAll with pagination
    @Query(value = "SELECT t FROM Template t LEFT JOIN FETCH t.category LEFT JOIN FETCH t.author LEFT JOIN FETCH t.previewImages",
           countQuery = "SELECT COUNT(t) FROM Template t")
    Page<Template> findAllWithCategoryAndAuthor(Pageable pageable);

    // Find a single template by ID with eager loading
    @Query("SELECT t FROM Template t LEFT JOIN FETCH t.category LEFT JOIN FETCH t.author LEFT JOIN FETCH t.previewImages WHERE t.id = :id")
    java.util.Optional<Template> findByIdWithCategoryAndAuthor(@Param("id") Long id);

    // Tìm top N template có status là PUBLISHED, sắp xếp theo downloads giảm dần
    @Query("SELECT t FROM Template t LEFT JOIN FETCH t.category LEFT JOIN FETCH t.author LEFT JOIN FETCH t.previewImages WHERE t.status = :status ORDER BY t.downloads DESC")
    List<Template> findTop10ByStatusOrderByDownloadsDesc(@Param("status") TemplateStatus status);
}