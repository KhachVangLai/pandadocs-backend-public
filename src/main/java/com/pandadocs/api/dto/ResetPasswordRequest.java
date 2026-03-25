package com.pandadocs.api.dto;

import jakarta.validation.constraints.NotBlank;
import com.pandadocs.api.validation.ValidPassword;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    @NotBlank
    private String token;

    @NotBlank
    @ValidPassword
    private String newPassword;
}