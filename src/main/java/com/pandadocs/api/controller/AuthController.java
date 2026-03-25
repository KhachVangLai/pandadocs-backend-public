package com.pandadocs.api.controller;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import com.pandadocs.api.dto.SignupRequest;
import com.pandadocs.api.dto.MessageResponse;
import com.pandadocs.api.model.ERole;
import com.pandadocs.api.model.Role;
import com.pandadocs.api.model.User;
import com.pandadocs.api.repository.RoleRepository;
import com.pandadocs.api.repository.UserRepository;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import com.pandadocs.api.dto.JwtResponse;
import com.pandadocs.api.dto.LoginRequest;
import com.pandadocs.api.security.jwt.JwtUtils;
import java.util.List;
import java.util.stream.Collectors;

import com.pandadocs.api.dto.ForgotPasswordRequest;
import com.pandadocs.api.dto.ResetPasswordRequest;
import java.util.UUID; // Thêm import này
import com.pandadocs.api.model.UserStatus;
import com.pandadocs.api.service.EmailService;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import jakarta.mail.MessagingException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
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
    // Kiểm tra username trùng
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
        return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
    }

    // Kiểm tra email đã tồn tại chưa
    User existingUser = userRepository.findByEmail(signUpRequest.getEmail()).orElse(null);

    if (existingUser != null) {
        if (existingUser.getStatus() == UserStatus.ACTIVE) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        } else {
            // Nếu user chưa xác minh, cấp lại token mới và gửi lại email xác minh
            String newToken = UUID.randomUUID().toString();
            existingUser.setVerificationToken(newToken);
            existingUser.setVerificationTokenExpiry(Instant.now().plusSeconds(3600 * 24)); // 24h
            userRepository.save(existingUser);

            String verificationLink = frontendBaseUrl + "/verify-email?token=" + newToken;
            String emailContent = "Bạn đã đăng ký PandaDocs nhưng chưa xác minh. Vui lòng nhấp vào liên kết sau để hoàn tất: "
                + "<a href=\"" + verificationLink + "\">Xác minh Email</a>";

            try {
                emailService.sendEmail(existingUser.getEmail(), "Xác minh lại Email PandaDocs", emailContent);
            } catch (MessagingException e) {
                System.err.println("Failed to resend verification email: " + e.getMessage());
            }

            return ResponseEntity.ok(new MessageResponse("Tài khoản này chưa xác minh. Hệ thống đã gửi lại email xác minh mới."));
        }
    }

    // === Nếu email hoàn toàn mới thì tạo user mới ===
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
        System.err.println("Failed to send verification email to " + user.getEmail() + ": " + e.getMessage());
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

    // Lấy thông tin chi tiết của user
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    List<String> roles = userDetails.getAuthorities().stream()
        .map(item -> item.getAuthority())
        .collect(Collectors.toList());

    // Lấy user từ DB để có email và id
    User user = userRepository.findByUsername(userDetails.getUsername()).get();

    // Check if user is verified
    if (user.getStatus() == UserStatus.UNVERIFIED) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Tài khoản của bạn chưa được xác minh. Vui lòng kiểm tra email để xác minh tài khoản."));
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
    // Với JWT, logic logout chính nằm ở phía client (xóa token).
    // API này chỉ đơn thuần trả về một xác nhận thành công.
    // Trong các hệ thống bảo mật cao hơn, chúng ta sẽ thêm token này vào blacklist
    // ở đây.
    return ResponseEntity.ok(new MessageResponse("Log out successful!"));
  }

  // API #1: Yêu cầu reset mật khẩu
  @PostMapping("/forgot-password")
  public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
    User user = userRepository.findByEmail(forgotPasswordRequest.getEmail()) // Cần thêm findByEmail vào UserRepository
        .orElse(null);

    if (user == null) {
      // Luôn trả về OK để tránh kẻ xấu dò email
      return ResponseEntity
          .ok(new MessageResponse("If an account with that email exists, a password reset link has been sent."));
    }

    // Tạo token ngẫu nhiên
    String token = UUID.randomUUID().toString();
    user.setResetPasswordToken(token);
    // Đặt thời gian hết hạn là 1 giờ sau
    user.setResetPasswordTokenExpiry(Instant.now().plusSeconds(3600));
    userRepository.save(user);

    // GỬI EMAIL
    String resetLink = frontendBaseUrl + "/reset-password?token=" + token;
    String emailContent = "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản PandaDocs của mình. Vui lòng nhấp vào liên kết sau để đặt lại mật khẩu: <a href=\"" + resetLink + "\">Đặt lại Mật khẩu</a>. Liên kết này sẽ hết hạn sau 1 giờ.";
    try {
        emailService.sendEmail(user.getEmail(), "Đặt lại Mật khẩu PandaDocs", emailContent);
    } catch (MessagingException e) {
        System.err.println("Failed to send password reset email to " + user.getEmail() + ": " + e.getMessage());
    }

    return ResponseEntity
        .ok(new MessageResponse("If an account with that email exists, a password reset link has been sent."));
  }

  // API #2: Thực hiện reset mật khẩu
  @PostMapping("/reset-password")
  public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
    // Tìm user bằng token
    User user = userRepository.findByResetPasswordToken(resetPasswordRequest.getToken()) // Cần thêm
                                                                                         // findByResetPasswordToken
        .orElse(null);

    // Kiểm tra token có hợp lệ và còn hạn không
    if (user == null || user.getResetPasswordTokenExpiry().isBefore(Instant.now())) {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid or expired password reset token."));
    }

    // Cập nhật mật khẩu mới
    user.setPassword(encoder.encode(resetPasswordRequest.getNewPassword()));

    // Xóa token sau khi đã sử dụng
    user.setResetPasswordToken(null);
    user.setResetPasswordTokenExpiry(null);
    userRepository.save(user);

    return ResponseEntity.ok(new MessageResponse("Password has been reset successfully!"));
  }
}