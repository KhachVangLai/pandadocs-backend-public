package com.pandadocs.api.controller;

import com.pandadocs.api.dto.NotificationDTO;
import com.pandadocs.api.model.Notification;
import com.pandadocs.api.repository.NotificationRepository;
import com.pandadocs.api.security.services.UserDetailsImpl;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors; // Thêm import

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    // Lấy tất cả thông báo của user đang đăng nhập
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<NotificationDTO>> getUserNotifications() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userDetails.getId());
        // Chuyển đổi từ List<Notification> sang List<NotificationDTO>
        List<NotificationDTO> dtos = notifications.stream().map(notification -> {
            NotificationDTO dto = new NotificationDTO();
            dto.setId(notification.getId());
            dto.setMessage(notification.getMessage());
            dto.setRead(notification.isRead());
            dto.setCreatedAt(notification.getCreatedAt());
            dto.setUsername(notification.getUser().getUsername());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
        // ---------------------
    }

    // Đánh dấu một thông báo là đã đọc
    @PutMapping("/{id}/read")
    @Transactional
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));

        // Thêm kiểm tra bảo mật để đảm bảo user chỉ sửa được thông báo của chính mình

        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok().build();
    }
}