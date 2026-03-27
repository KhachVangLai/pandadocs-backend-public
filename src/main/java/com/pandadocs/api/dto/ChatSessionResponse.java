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
    private String sessionId;
    private String conversationTitle;
    private Integer messageCount;
    private Instant createdAt;
    private List<ChatMessageDTO> messages; // Optional; present only when requested.

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatMessageDTO {
        private String role;
        private String content;
        private Instant timestamp;
    }
}
