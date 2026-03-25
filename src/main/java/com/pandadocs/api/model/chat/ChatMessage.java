package com.pandadocs.api.model.chat;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * In-memory chat message model (not persisted to database)
 * Represents a single message in a conversation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
  /**
   * Role of the message sender
   * USER: Message from user
   * ASSISTANT: Message from Gemini AI
   * SYSTEM: System messages (e.g., error messages)
   */
  private MessageRole role;

  /**
   * Content of the message
   */
  private String content;

  /**
   * Timestamp when message was created
   */
  @Builder.Default
  private Instant timestamp = Instant.now();

  /**
   * Optional: Template IDs mentioned in this message (for AI recommendations)
   */
  private java.util.List<Long> templateIds;

  /**
   * Optional: Action buttons attached to this message
   */
  private java.util.List<ActionButton> actionButtons;

  public enum MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ActionButton {
    private ActionType type;
    private String label;
    private Long templateId;

    public enum ActionType {
      BUY_NOW,        // Mua template ngay
      ADD_TO_LIBRARY, // Thêm template miễn phí vào library
      VIEW_DETAILS,   // Xem chi tiết template
      CANCEL          // Hủy action
    }
  }
}
