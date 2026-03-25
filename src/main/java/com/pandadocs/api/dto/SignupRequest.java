package com.pandadocs.api.dto;

import java.util.Set;
import jakarta.validation.constraints.*;
import com.pandadocs.api.validation.ValidPassword;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    @NotBlank
    @Size(min = 3, max = 20)
    private String username;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    @NotBlank
    @ValidPassword
    private String password;

    // role có thể không cần gửi lên, sẽ gán mặc định là USER
    private Set<String> role;
}