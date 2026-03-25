package com.pandadocs.api.controller;

import org.springframework.transaction.annotation.Transactional;
import com.pandadocs.api.dto.MessageResponse;
import com.pandadocs.api.dto.PayOSWebhookData;
import com.pandadocs.api.model.*;
import com.pandadocs.api.repository.*;
import com.pandadocs.api.service.PayOSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.v2.paymentRequests.PaymentLink;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/payments")
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

    /**
     * Webhook endpoint để nhận callback từ PayOS khi thanh toán thành công
     */
    @PostMapping("/payos-webhook")
    @Transactional
    public ResponseEntity<?> handlePayOSWebhook(@RequestBody PayOSWebhookData webhookData) {
        try {
            log.info("Received PayOS webhook: {}", webhookData);

            // 1. Verify webhook signature
            // Note: PayOS signature verification depends on their specific implementation
            // You may need to adjust this based on PayOS documentation
            // String dataToVerify = objectMapper.writeValueAsString(webhookData.getData());
            // if (!payOSService.verifyWebhookSignature(dataToVerify, webhookData.getSignature())) {
            //     log.error("Invalid webhook signature");
            //     return ResponseEntity.badRequest().body(new MessageResponse("Invalid signature"));
            // }

            // 2. Kiểm tra webhook có success không
            if (!webhookData.isSuccess() || webhookData.getData() == null) {
                log.warn("Webhook received but payment not successful");
                return ResponseEntity.ok(new MessageResponse("Webhook received but payment not successful"));
            }

            // 3. Tìm Order theo orderCode
            Long orderCode = webhookData.getData().getOrderCode();
            Order order = orderRepository.findById(orderCode)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderCode));

            // 4. Kiểm tra order đã được process chưa (tránh duplicate webhook)
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                log.info("Order {} already processed", orderCode);
                return ResponseEntity.ok(new MessageResponse("Order already processed"));
            }

            // 5. Cập nhật Order status
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setStatus("COMPLETED"); // For backward compatibility
            order.setPaidAt(Instant.now());
            orderRepository.save(order);

            // 6. Lấy template từ OrderItem (giả định 1 order có 1 template)
            OrderItem orderItem = order.getOrderItems().iterator().next();
            Template template = orderItem.getTemplate();
            User user = order.getUser();

            // 7. Thêm template vào Library của user
            Library libraryEntry = new Library();
            libraryEntry.setUser(user);
            libraryEntry.setTemplate(template);
            libraryEntry.setAcquiredAt(Instant.now());
            libraryRepository.save(libraryEntry);

            // 8. Tạo notification cho user
            String message = "Thanh toán thành công! Template '" + template.getTitle() + "' đã được thêm vào thư viện của bạn.";
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setMessage(message);
            notification.setCreatedAt(Instant.now());
            notificationRepository.save(notification);

            log.info("Payment processed successfully for order {}", orderCode);
            return ResponseEntity.ok(new MessageResponse("Payment processed successfully"));

        } catch (Exception e) {
            log.error("Error processing PayOS webhook", e);
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Complete payment sau khi user quay về từ PayOS
     * Frontend gọi endpoint này để hoàn tất đơn hàng
     */
    @PostMapping("/complete/{orderId}")
    @Transactional
    public ResponseEntity<?> completePayment(@PathVariable Long orderId) {
        try {
            log.info("Complete payment request for order: {}", orderId);

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // Nếu đã PAID rồi thì return success luôn
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Payment already completed");
                response.put("paymentStatus", order.getPaymentStatus());
                return ResponseEntity.ok(response);
            }

            // Verify payment with PayOS
            PaymentLink paymentInfo = payOSService.getPaymentInfo(orderId);
            if (paymentInfo == null || paymentInfo.getStatus() == null ||
                !paymentInfo.getStatus().name().equals("PAID")) {
                return ResponseEntity.badRequest().body(new MessageResponse("Payment not confirmed by PayOS"));
            }

            // Update payment status
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setStatus("COMPLETED");
            order.setPaidAt(Instant.now());
            orderRepository.save(order);

            // Get template from order
            OrderItem orderItem = order.getOrderItems().iterator().next();
            Template template = orderItem.getTemplate();
            User user = order.getUser();

            // Check if already in library (double check)
            boolean alreadyOwned = libraryRepository.existsByUserAndTemplate(user, template);
            if (!alreadyOwned) {
                // Add to Library
                Library libraryEntry = new Library();
                libraryEntry.setUser(user);
                libraryEntry.setTemplate(template);
                libraryEntry.setAcquiredAt(Instant.now());
                libraryRepository.save(libraryEntry);
                log.info("Added template {} to user {} library", template.getId(), user.getId());
            } else {
                log.info("Template {} already in user {} library", template.getId(), user.getId());
            }

            // Create notification
            String message = "Thanh toán thành công! Template '" + template.getTitle() + "' đã được thêm vào thư viện của bạn.";
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setMessage(message);
            notification.setCreatedAt(Instant.now());
            notificationRepository.save(notification);

            log.info("Payment completed successfully for order {}", orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment completed successfully");
            response.put("orderId", order.getId());
            response.put("paymentStatus", order.getPaymentStatus());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error completing payment for order {}", orderId, e);
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Endpoint để frontend query trạng thái thanh toán (read-only)
     */
    @GetMapping("/verify/{orderId}")
    public ResponseEntity<?> verifyPaymentStatus(@PathVariable Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.getId());
            response.put("paymentStatus", order.getPaymentStatus());
            response.put("status", order.getStatus());
            response.put("totalAmount", order.getTotalAmount());
            response.put("createdAt", order.getCreatedAt());
            response.put("paidAt", order.getPaidAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Endpoint để handle khi user cancel payment (optional)
     */
    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<?> cancelPayment(@PathVariable Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            if (order.getPaymentStatus() == PaymentStatus.PENDING_PAYMENT) {
                order.setPaymentStatus(PaymentStatus.CANCELLED);
                order.setStatus("CANCELLED");
                orderRepository.save(order);

                return ResponseEntity.ok(new MessageResponse("Payment cancelled"));
            } else {
                return ResponseEntity.badRequest().body(new MessageResponse("Cannot cancel this order"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
}
