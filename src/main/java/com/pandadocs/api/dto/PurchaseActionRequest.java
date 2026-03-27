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
    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @NotNull(message = "Action is required")
    private ChatActionButtonDTO.ActionType action;

    @NotNull(message = "Template ID is required")
    private Long templateId;
}
