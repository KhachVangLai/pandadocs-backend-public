package com.pandadocs.api.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class SendNotificationRequest {
    private String message;
    private List<Long> recipientIds; // Recipient user IDs.
    private boolean sendToAll = false; // Sends to all users when true.
}
