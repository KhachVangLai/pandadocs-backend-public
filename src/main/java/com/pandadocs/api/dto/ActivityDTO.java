package com.pandadocs.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class ActivityDTO {
    private String type;
    private String description;
    private Instant timestamp;
}