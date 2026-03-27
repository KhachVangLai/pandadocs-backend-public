package com.pandadocs.api.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandadocs.api.dto.MessageResponse;
import com.pandadocs.api.dto.PayOSWebhookData;
import com.pandadocs.api.model.ERole;
import com.pandadocs.api.model.Library;
import com.pandadocs.api.model.Notification;
import com.pandadocs.api.model.Order;
import com.pandadocs.api.model.OrderItem;
import com.pandadocs.api.model.PaymentStatus;
import com.pandadocs.api.model.Template;
import com.pandadocs.api.model.User;
import com.pandadocs.api.repository.LibraryRepository;
import com.pandadocs.api.repository.NotificationRepository;
import com.pandadocs.api.repository.OrderRepository;
import com.pandadocs.api.security.services.UserDetailsImpl;
import com.pandadocs.api.service.PayOSService;

import lombok.extern.slf4j.Slf4j;
import vn.payos.model.v2.paymentRequests.PaymentLink;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class PaymentController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PayOSService payOSService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/payos-webhook")
    @Transactional
    public ResponseEntity<?> handlePayOSWebhook(@RequestBody PayOSWebhookData webhookData) {
        try {
            String dataToVerify = objectMapper.writeValueAsString(webhookData.getData());
            if (!payOSService.verifyWebhookSignature(dataToVerify, webhookData.getSignature())) {
                log.warn("Rejected PayOS webhook due to invalid signature");
                return ResponseEntity.badRequest().body(new MessageResponse("Invalid signature"));
            }

            if (!webhookData.isSuccess() || webhookData.getData() == null) {
                return ResponseEntity.ok(new MessageResponse("Webhook received but payment not successful"));
            }

            Long orderCode = webhookData.getData().getOrderCode();
            log.info("Received verified PayOS webhook for order {}", orderCode);

            Order order = orderRepository.findById(orderCode)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderCode));

            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                return ResponseEntity.ok(new MessageResponse("Order already processed"));
            }

            order.setPaymentStatus(PaymentStatus.PAID);
            order.setStatus("COMPLETED");
            order.setPaidAt(Instant.now());
            orderRepository.save(order);

            OrderItem orderItem = order.getOrderItems().stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Order has no items"));
            Template template = orderItem.getTemplate();
            User user = order.getUser();

            if (!libraryRepository.existsByUserAndTemplate(user, template)) {
                Library libraryEntry = new Library();
                libraryEntry.setUser(user);
                libraryEntry.setTemplate(template);
                libraryEntry.setAcquiredAt(Instant.now());
                libraryRepository.save(libraryEntry);
            }

            Notification notification = new Notification();
            notification.setUser(user);
            notification.setMessage("Thanh toán thành công! Template '" + template.getTitle() + "' đã được thêm vào thư viện của bạn.");
            notification.setCreatedAt(Instant.now());
            notificationRepository.save(notification);

            return ResponseEntity.ok(new MessageResponse("Payment processed successfully"));
        } catch (Exception e) {
            log.error("Error processing PayOS webhook", e);
            return ResponseEntity.badRequest().body(new MessageResponse("Error processing webhook"));
        }
    }

    @PostMapping("/complete/{orderId}")
    @Transactional
    public ResponseEntity<?> completePayment(@PathVariable Long orderId) {
        try {
            Order order = requireAccessibleOrder(orderId);

            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Payment already completed");
                response.put("paymentStatus", order.getPaymentStatus());
                return ResponseEntity.ok(response);
            }

            PaymentLink paymentInfo = payOSService.getPaymentInfo(orderId);
            if (paymentInfo == null || paymentInfo.getStatus() == null || !paymentInfo.getStatus().name().equals("PAID")) {
                return ResponseEntity.badRequest().body(new MessageResponse("Payment not confirmed by PayOS"));
            }

            order.setPaymentStatus(PaymentStatus.PAID);
            order.setStatus("COMPLETED");
            order.setPaidAt(Instant.now());
            orderRepository.save(order);

            OrderItem orderItem = order.getOrderItems().stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Order has no items"));
            Template template = orderItem.getTemplate();
            User user = order.getUser();

            if (!libraryRepository.existsByUserAndTemplate(user, template)) {
                Library libraryEntry = new Library();
                libraryEntry.setUser(user);
                libraryEntry.setTemplate(template);
                libraryEntry.setAcquiredAt(Instant.now());
                libraryRepository.save(libraryEntry);
                log.info("Added template {} to user {} library", template.getId(), user.getId());
            }

            Notification notification = new Notification();
            notification.setUser(user);
            notification.setMessage("Thanh toán thành công! Template '" + template.getTitle() + "' đã được thêm vào thư viện của bạn.");
            notification.setCreatedAt(Instant.now());
            notificationRepository.save(notification);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment completed successfully");
            response.put("orderId", order.getId());
            response.put("paymentStatus", order.getPaymentStatus());
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error completing payment for order {}", orderId, e);
            return ResponseEntity.badRequest().body(new MessageResponse("Error completing payment"));
        }
    }

    @GetMapping("/verify/{orderId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> verifyPaymentStatus(@PathVariable Long orderId) {
        try {
            Order order = requireAccessibleOrder(orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.getId());
            response.put("paymentStatus", order.getPaymentStatus());
            response.put("status", order.getStatus());
            response.put("totalAmount", order.getTotalAmount());
            response.put("createdAt", order.getCreatedAt());
            response.put("paidAt", order.getPaidAt());

            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error verifying payment status for order {}", orderId, e);
            return ResponseEntity.badRequest().body(new MessageResponse("Error verifying payment status"));
        }
    }

    @PostMapping("/cancel/{orderId}")
    @Transactional
    public ResponseEntity<?> cancelPayment(@PathVariable Long orderId) {
        try {
            Order order = requireAccessibleOrder(orderId);

            if (order.getPaymentStatus() == PaymentStatus.PENDING_PAYMENT) {
                order.setPaymentStatus(PaymentStatus.CANCELLED);
                order.setStatus("CANCELLED");
                orderRepository.save(order);
                return ResponseEntity.ok(new MessageResponse("Payment cancelled"));
            }

            return ResponseEntity.badRequest().body(new MessageResponse("Cannot cancel this order"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error cancelling payment for order {}", orderId, e);
            return ResponseEntity.badRequest().body(new MessageResponse("Error cancelling payment"));
        }
    }

    private Order requireAccessibleOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        UserDetailsImpl currentUser = getAuthenticatedUser();
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(ERole.ROLE_ADMIN.name()));

        if (!isAdmin && !order.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Error: You are not authorized to access this order.");
        }

        return order;
    }

    private UserDetailsImpl getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        if (!(principal instanceof UserDetailsImpl userDetails)) {
            throw new AccessDeniedException("Authentication required");
        }
        return userDetails;
    }
}
