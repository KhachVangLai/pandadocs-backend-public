package com.pandadocs.api.dto;

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
public class ChatMessageResponse {
    /**
     * Session ID for this conversation
     */
    private String sessionId;

    /**
     * AI's response message
     */
    private String message;

    /**
     * Templates recommended by AI (if any)
     */
    private List<TemplateCardDTO> templates;

    /**
     * Action buttons (if AI is asking user to confirm an action)
     */
    private List<ChatActionButtonDTO> actionButtons;

    /**
     * Auto-generated conversation title (null if not generated yet)
     */
    private String conversationTitle;
}
