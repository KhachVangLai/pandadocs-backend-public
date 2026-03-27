package com.pandadocs.api.dto;

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
public class ChatActionButtonDTO {
    private ActionType type;

    // Label shown to the user.
    private String label;

    private Long templateId;

    public enum ActionType {
        BUY_NOW,
        ADD_TO_LIBRARY,
        VIEW_DETAILS,
        CANCEL
    }
}
