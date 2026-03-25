package com.pandadocs.api.repository;

import com.pandadocs.api.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByTemplateId(Long templateId);

    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.template.id = :templateId")
    List<Review> findByTemplateIdWithUser(@Param("templateId") Long templateId);

    @Query("SELECT r FROM Review r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.template WHERE r.user.id = :userId")
    List<Review> findUserReviews(@Param("userId") Long userId);

    // Xóa tất cả reviews của một template
    @Modifying
    @Transactional
    void deleteByTemplateId(Long templateId);

    // Tính average rating của một template
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.template.id = :templateId")
    Double calculateAverageRating(@Param("templateId") Long templateId);

    // Đếm số lượng reviews của một template
    @Query("SELECT COUNT(r) FROM Review r WHERE r.template.id = :templateId")
    Long countByTemplateId(@Param("templateId") Long templateId);
}