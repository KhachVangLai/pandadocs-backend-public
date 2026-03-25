package com.pandadocs.api.repository;

import com.pandadocs.api.model.Library;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.pandadocs.api.model.Template;
import com.pandadocs.api.model.User;

@Repository
public interface LibraryRepository extends JpaRepository<Library, Long> {
    // Tìm tất cả các library item của một user theo user ID
    List<Library> findByUserId(Long userId);

    // Eager load Template với previewImages (chỉ load 1 @ElementCollection để tránh MultipleBagFetchException)
    // format và industry sẽ được lazy load trong transaction
    @Query("SELECT DISTINCT l FROM Library l " +
           "LEFT JOIN FETCH l.template t " +
           "LEFT JOIN FETCH t.category " +
           "LEFT JOIN FETCH t.author " +
           "LEFT JOIN FETCH t.previewImages " +
           "WHERE l.user.id = :userId")
    List<Library> findByUserIdWithTemplateDetails(@Param("userId") Long userId);

    // <-- THÊM HÀM NÀY
    // Kiểm tra xem có tồn tại bản ghi nào khớp với user và template không
    boolean existsByUserAndTemplate(User user, Template template);

    // Xóa tất cả library entries của một template
    @Modifying
    @Transactional
    void deleteByTemplateId(Long templateId);
}