package com.pandadocs.api.repository;

import com.pandadocs.api.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Lấy thông báo của user, sắp xếp mới nhất lên đầu
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
}