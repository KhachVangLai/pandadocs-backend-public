package com.pandadocs.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseActionRequest {
    /**
     * Session ID of the chat
     */
    @NotBlank(message = "Session ID is required")
    private String sessionId;

    /**
     * Action type (BUY_NOW, ADD_TO_LIBRARY, VIEW_DETAILS, CANCEL)
     */
    @NotNull(message = "Action is required")
    private ChatActionButtonDTO.ActionType action;

    /**
     * Template ID for the action
     */
    @NotNull(message = "Template ID is required")
    private Long templateId;
}
