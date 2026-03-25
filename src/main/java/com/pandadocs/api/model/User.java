package com.pandadocs.api.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", 
       uniqueConstraints = { 
           @UniqueConstraint(columnNames = "username"),
           @UniqueConstraint(columnNames = "email") 
       })
@Getter
@Setter
@NoArgsConstructor
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Size(max = 20)
  private String username;

  @NotBlank
  @Size(max = 50)
  @Email
  private String email;

  @NotBlank
  @Size(max = 120)
  private String password;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "user_roles", 
             joinColumns = @JoinColumn(name = "user_id"), 
             inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles = new HashSet<>();

  @Size(max = 100)
  private String name;

  private String avatar;

  @Enumerated(EnumType.STRING)
  private UserStatus status;

  private Instant createdAt;

  // ---- GOOGLE OAUTH2 ----
  @Column(unique = true)
  private String googleId; // Google user ID (sub)
  // -----------------------

  // ---- CÁC TRƯỜNG MỚI CHO TÍNH NĂNG RESET PASSWORD ----
  private String resetPasswordToken;

  private Instant resetPasswordTokenExpiry;
  // ----------------------------------------------------

  // ---- EMAIL VERIFICATION FIELDS ----
  private String verificationToken;

  private Instant verificationTokenExpiry;
  // -----------------------------------

  public User(String username, String email, String password) {
    this.username = username;
    this.email = email;
    this.password = password;
    this.createdAt = Instant.now();
    this.status = UserStatus.UNVERIFIED; // Default status is UNVERIFIED
  }
}