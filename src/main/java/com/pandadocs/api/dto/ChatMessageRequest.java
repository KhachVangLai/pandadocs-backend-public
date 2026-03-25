package com.pandadocs.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    /**
     * Session ID (optional, will be created if null)
     */
    @Size(max = 100)
    private String sessionId;

    /**
     * User's message to send to AI
     */
    @NotBlank(message = "Message cannot be empty")
    @Size(max = 500, message = "Message too long (max 500 characters)")
    private String message;
}
