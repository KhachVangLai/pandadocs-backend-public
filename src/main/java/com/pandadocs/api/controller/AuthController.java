package com.pandadocs.api.controller;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pandadocs.api.dto.ForgotPasswordRequest;
import com.pandadocs.api.dto.JwtResponse;
import com.pandadocs.api.dto.LoginRequest;
import com.pandadocs.api.dto.MessageResponse;
import com.pandadocs.api.dto.ResetPasswordRequest;
import com.pandadocs.api.dto.SignupRequest;
import com.pandadocs.api.model.ERole;
import com.pandadocs.api.model.Role;
import com.pandadocs.api.model.User;
import com.pandadocs.api.model.UserStatus;
import com.pandadocs.api.repository.RoleRepository;
import com.pandadocs.api.repository.UserRepository;
import com.pandadocs.api.security.jwt.JwtUtils;
import com.pandadocs.api.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

  @Autowired
  UserRepository userRepository;

  @Autowired
  RoleRepository roleRepository;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  AuthenticationManager authenticationManager;

  @Autowired
  JwtUtils jwtUtils;

  @Autowired
  EmailService emailService;

  @Value("${app.frontend.base-url}")
  private String frontendBaseUrl;

  @PostMapping("/signup")
  public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
    }

    User existingUser = userRepository.findByEmail(signUpRequest.getEmail()).orElse(null);

    if (existingUser != null) {
      if (existingUser.getStatus() == UserStatus.ACTIVE) {
        return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
      } else {
        String newToken = UUID.randomUUID().toString();
        existingUser.setVerificationToken(newToken);
        existingUser.setVerificationTokenExpiry(Instant.now().plusSeconds(3600 * 24));
        userRepository.save(existingUser);

        String verificationLink = frontendBaseUrl + "/verify-email?token=" + newToken;
        String emailContent = "Bạn đã đăng ký PandaDocs nhưng chưa xác minh. Vui lòng nhấp vào liên kết sau để hoàn tất: "
            + "<a href=\"" + verificationLink + "\">Xác minh Email</a>";

        try {
          emailService.sendEmail(existingUser.getEmail(), "Xác minh lại Email PandaDocs", emailContent);
        } catch (MessagingException e) {
          log.warn("Failed to resend verification email for user {}", existingUser.getId(), e);
        }

        return ResponseEntity.ok(new MessageResponse("Tài khoản này chưa xác minh. Hệ thống đã gửi lại email xác minh mới."));
      }
    }

    User user = new User(signUpRequest.getUsername(),
        signUpRequest.getEmail(),
        encoder.encode(signUpRequest.getPassword()));

    Set<Role> roles = new HashSet<>();
    Role userRole = roleRepository.findByName(ERole.ROLE_USER)
        .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
    roles.add(userRole);
    user.setRoles(roles);

    user.setStatus(UserStatus.UNVERIFIED);
    String verificationToken = UUID.randomUUID().toString();
    user.setVerificationToken(verificationToken);
    user.setVerificationTokenExpiry(Instant.now().plusSeconds(3600 * 24));
    userRepository.save(user);

    String verificationLink = frontendBaseUrl + "/verify-email?token=" + verificationToken;
    String emailContent = "Chào mừng bạn đến với PandaDocs! Vui lòng nhấp vào liên kết sau để xác minh địa chỉ email của bạn: "
        + "<a href=\"" + verificationLink + "\">Xác minh Email</a>";

    try {
      emailService.sendEmail(user.getEmail(), "Xác minh Email của bạn cho PandaDocs", emailContent);
    } catch (MessagingException e) {
      log.warn("Failed to send verification email for user {}", user.getId(), e);
    }

    return ResponseEntity.ok(new MessageResponse("Đăng ký thành công! Vui lòng kiểm tra email để xác minh tài khoản."));
  }

  @GetMapping("/verify-email")
  public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
    User user = userRepository.findByVerificationToken(token)
        .orElse(null);

    if (user == null) {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid verification token."));
    }

    if (user.getVerificationTokenExpiry().isBefore(Instant.now())) {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Verification token has expired."));
    }

    user.setStatus(UserStatus.ACTIVE);
    user.setVerificationToken(null);
    user.setVerificationTokenExpiry(null);
    userRepository.save(user);

    return ResponseEntity.ok(new MessageResponse("Email verified successfully! You can now log in."));
  }

  @PostMapping("/signin")
  public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);
    String jwt = jwtUtils.generateJwtToken(authentication);

    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    List<String> roles = userDetails.getAuthorities().stream()
        .map(item -> item.getAuthority())
        .collect(Collectors.toList());

    User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
    if (user.getStatus() == UserStatus.UNVERIFIED) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
          new MessageResponse("Tài khoản của bạn chưa được xác minh. Vui lòng kiểm tra email để xác minh tài khoản."));
    }

    return ResponseEntity.ok(new JwtResponse(jwt,
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        roles));
  }

  @PostMapping("/logout")
  @PreAuthorize("hasRole('USER') or hasRole('SELLER') or hasRole('ADMIN')")
  public ResponseEntity<?> logoutUser() {
    return ResponseEntity.ok(new MessageResponse("Log out successful!"));
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
    User user = userRepository.findByEmail(forgotPasswordRequest.getEmail())
        .orElse(null);

    if (user == null) {
      return ResponseEntity
          .ok(new MessageResponse("If an account with that email exists, a password reset link has been sent."));
    }

    String token = UUID.randomUUID().toString();
    user.setResetPasswordToken(token);
    user.setResetPasswordTokenExpiry(Instant.now().plusSeconds(3600));
    userRepository.save(user);

    String resetLink = frontendBaseUrl + "/reset-password?token=" + token;
    String emailContent = "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản PandaDocs của mình. Vui lòng nhấp vào liên kết sau để đặt lại mật khẩu: <a href=\""
        + resetLink
        + "\">Đặt lại Mật khẩu</a>. Liên kết này sẽ hết hạn sau 1 giờ.";
    try {
      emailService.sendEmail(user.getEmail(), "Đặt lại Mật khẩu PandaDocs", emailContent);
    } catch (MessagingException e) {
      log.warn("Failed to send password reset email for user {}", user.getId(), e);
    }

    return ResponseEntity
        .ok(new MessageResponse("If an account with that email exists, a password reset link has been sent."));
  }

  @PostMapping("/reset-password")
  public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
    User user = userRepository.findByResetPasswordToken(resetPasswordRequest.getToken())
        .orElse(null);

    if (user == null || user.getResetPasswordTokenExpiry().isBefore(Instant.now())) {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid or expired password reset token."));
    }

    user.setPassword(encoder.encode(resetPasswordRequest.getNewPassword()));
    user.setResetPasswordToken(null);
    user.setResetPasswordTokenExpiry(null);
    userRepository.save(user);

    return ResponseEntity.ok(new MessageResponse("Password has been reset successfully!"));
  }
}
