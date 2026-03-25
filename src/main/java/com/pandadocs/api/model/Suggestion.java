package com.pandadocs.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "suggestions")
@Getter
@Setter
@NoArgsConstructor
public class Suggestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    private SuggestionStatus status = SuggestionStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String response;

    private Instant createdAt;
    private Instant respondedAt;
}