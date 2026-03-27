package com.pandadocs.api.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class ReviewDTO {
    private Long id;
    private int rating;
    private String comment;
    private Instant createdAt;
    private String username; // Reviewer username only.
}
