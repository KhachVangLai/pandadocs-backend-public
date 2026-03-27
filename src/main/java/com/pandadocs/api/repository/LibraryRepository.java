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
    List<Library> findByUserId(Long userId);

    // Fetch preview images eagerly and keep the other element collections lazy to
    // avoid MultipleBagFetchException.
    @Query("SELECT DISTINCT l FROM Library l " +
           "LEFT JOIN FETCH l.template t " +
           "LEFT JOIN FETCH t.category " +
           "LEFT JOIN FETCH t.author " +
           "LEFT JOIN FETCH t.previewImages " +
           "WHERE l.user.id = :userId")
    List<Library> findByUserIdWithTemplateDetails(@Param("userId") Long userId);

    boolean existsByUserAndTemplate(User user, Template template);

    @Modifying
    @Transactional
    void deleteByTemplateId(Long templateId);
}
