package com.pandadocs.api.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSessionResponse {
    /**
     * Session ID
     */
    private String sessionId;

    /**
     * Conversation title (auto-generated)
     */
    private String conversationTitle;

    /**
     * Number of messages in this session
     */
    private Integer messageCount;

    /**
     * When session was created
     */
    private Instant createdAt;

    /**
     * List of messages (optional, can be null if not requested)
     */
    private List<ChatMessageDTO> messages;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatMessageDTO {
        private String role; // USER or ASSISTANT
        private String content;
        private Instant timestamp;
    }
}
