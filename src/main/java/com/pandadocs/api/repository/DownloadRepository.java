package com.pandadocs.api.repository;

import com.pandadocs.api.model.Download;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.time.Instant; // Thêm import
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface DownloadRepository extends JpaRepository<Download, Long> {
    @Query("SELECT d FROM Download d LEFT JOIN FETCH d.user LEFT JOIN FETCH d.template WHERE d.user.id = :userId")
    List<Download> findUserDownloads(@Param("userId") Long userId);
    long countByTimestampAfter(Instant startDate);

    // Xóa tất cả downloads của một template
    @Modifying
    @Transactional
    void deleteByTemplateId(Long templateId);
}