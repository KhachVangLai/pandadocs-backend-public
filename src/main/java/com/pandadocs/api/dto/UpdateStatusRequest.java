package com.pandadocs.api.dto;

import com.pandadocs.api.model.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStatusRequest {
    @NotNull
    private UserStatus status;
}