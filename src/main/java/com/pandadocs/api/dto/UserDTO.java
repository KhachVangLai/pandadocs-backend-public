package com.pandadocs.api.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.Set;

@Getter
@Setter
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String name;
    private String avatar;
    private String status;
    private Instant createdAt;
    private Set<String> roles;
}