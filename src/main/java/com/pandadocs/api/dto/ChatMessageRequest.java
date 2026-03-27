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
    // Optional; a new session is created when omitted.
    @Size(max = 100)
    private String sessionId;

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 500, message = "Message too long (max 500 characters)")
    private String message;
}
