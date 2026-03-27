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
    private String sessionId;
    private String message;
    private List<TemplateCardDTO> templates;
    private List<ChatActionButtonDTO> actionButtons;
    private String conversationTitle;
}
