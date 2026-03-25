package com.pandadocs.api.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class LibraryItemDTO {
    private Long libraryId;
    private TemplateDTO template;
    private Instant acquiredAt;
}