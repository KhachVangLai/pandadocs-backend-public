package com.pandadocs.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "templates")
@Getter
@Setter
@NoArgsConstructor
public class Template {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double price;

    private String fileUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Enumerated(EnumType.STRING)
    private TemplateStatus status;

    @ElementCollection
    private List<String> format;

    @ElementCollection
    private List<String> industry;

    private boolean isPremium;

    private float rating = 0;

    private int reviewCount = 0;

    private int downloads = 0;
    
    private Instant createdAt;

    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "template_images", joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "image_url")
    private List<String> previewImages;
}
