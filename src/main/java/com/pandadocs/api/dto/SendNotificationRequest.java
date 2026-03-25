package com.pandadocs.api.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class SendNotificationRequest {
    private String message;
    private List<Long> recipientIds; // Danh sách ID của những người nhận
    private boolean sendToAll = false; // Gửi cho tất cả user nếu true
}