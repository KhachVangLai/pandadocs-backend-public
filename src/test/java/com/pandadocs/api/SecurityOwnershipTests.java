package com.pandadocs.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.pandadocs.api.model.Notification;
import com.pandadocs.api.model.Order;
import com.pandadocs.api.model.PaymentStatus;
import com.pandadocs.api.model.User;
import com.pandadocs.api.model.UserStatus;
import com.pandadocs.api.repository.NotificationRepository;
import com.pandadocs.api.repository.OrderRepository;
import com.pandadocs.api.repository.UserRepository;
import com.pandadocs.api.security.services.UserDetailsImpl;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityOwnershipTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        notificationRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void markAsReadRejectsDifferentUser() throws Exception {
        User owner = saveUser("owner-notification", "owner-notification@example.com");
        User otherUser = saveUser("other-notification", "other-notification@example.com");

        Notification notification = new Notification();
        notification.setUser(owner);
        notification.setMessage("Notification for owner");
        notification.setCreatedAt(Instant.now());
        notification = notificationRepository.save(notification);

        mockMvc.perform(put("/api/notifications/{id}/read", notification.getId())
                        .with(user(principal(otherUser))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Error: You are not authorized to update this notification."));
    }

    @Test
    void markAsReadAllowsOwner() throws Exception {
        User owner = saveUser("owner-self", "owner-self@example.com");

        Notification notification = new Notification();
        notification.setUser(owner);
        notification.setMessage("Notification for owner");
        notification.setCreatedAt(Instant.now());
        notification = notificationRepository.save(notification);

        mockMvc.perform(put("/api/notifications/{id}/read", notification.getId())
                        .with(user(principal(owner))))
                .andExpect(status().isOk());
    }

    @Test
    void paymentEndpointsRejectDifferentUser() throws Exception {
        User owner = saveUser("owner-payment", "owner-payment@example.com");
        User otherUser = saveUser("other-payment", "other-payment@example.com");
        Order order = saveOrder(owner);

        mockMvc.perform(get("/api/payments/verify/{orderId}", order.getId())
                        .with(user(principal(otherUser))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/payments/cancel/{orderId}", order.getId())
                        .with(user(principal(otherUser))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/payments/complete/{orderId}", order.getId())
                        .with(user(principal(otherUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    void paymentVerifyAllowsOwner() throws Exception {
        User owner = saveUser("owner-verify", "owner-verify@example.com");
        Order order = saveOrder(owner);

        mockMvc.perform(get("/api/payments/verify/{orderId}", order.getId())
                        .with(user(principal(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.getId()));
    }

    @Test
    void payosWebhookRejectsInvalidSignatureWithoutAuthentication() throws Exception {
        String payload = """
                {
                  "code": "00",
                  "desc": "success",
                  "success": true,
                  "data": {
                    "orderCode": 99,
                    "amount": 1000,
                    "description": "Test order"
                  },
                  "signature": "invalid-signature"
                }
                """;

        mockMvc.perform(post("/api/payments/payos-webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid signature"));
    }

    private User saveUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    private Order saveOrder(User owner) {
        Order order = new Order();
        order.setUser(owner);
        order.setTotalAmount(1000.0);
        order.setCreatedAt(Instant.now());
        order.setStatus("PENDING");
        order.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        return orderRepository.save(order);
    }

    private UserDetailsImpl principal(User user) {
        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getStatus(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}