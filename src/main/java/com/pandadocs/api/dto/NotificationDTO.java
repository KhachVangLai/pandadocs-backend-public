package com.pandadocs.api.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class NotificationDTO {
    private Long id;
    private String message;
    private boolean isRead;
    private Instant createdAt;
    private String username; // Recipient username only.
}
