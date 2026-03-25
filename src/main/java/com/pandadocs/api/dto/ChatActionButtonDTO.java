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
    /**
     * Type of action
     */
    private ActionType type;

    /**
     * Button label text (e.g., "Mua ngay", "Thêm vào library")
     */
    private String label;

    /**
     * Template ID for this action
     */
    private Long templateId;

    public enum ActionType {
        BUY_NOW,        // Mua template
        ADD_TO_LIBRARY, // Thêm miễn phí vào library
        VIEW_DETAILS,   // Xem chi tiết template
        CANCEL          // Hủy
    }
}
