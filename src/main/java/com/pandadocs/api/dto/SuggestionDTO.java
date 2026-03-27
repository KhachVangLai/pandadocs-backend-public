package com.pandadocs.api.dto;

import com.pandadocs.api.model.SuggestionStatus;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class SuggestionDTO {
    private Long id;
    private String message;
    private SuggestionStatus status;
    private String response;
    private Instant createdAt;
    private Instant respondedAt;
    private String username; // Username of the submitting user.
}
