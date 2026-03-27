package com.pandadocs.api.controller;

import java.time.Instant;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pandadocs.api.config.PayOSConfig;
import com.pandadocs.api.dto.MessageResponse;
import com.pandadocs.api.dto.PurchaseRequest;
import com.pandadocs.api.dto.PurchaseResponse;
import com.pandadocs.api.model.Library;
import com.pandadocs.api.model.Order;
import com.pandadocs.api.model.OrderItem;
import com.pandadocs.api.model.PaymentStatus;
import com.pandadocs.api.model.Template;
import com.pandadocs.api.model.User;
import com.pandadocs.api.repository.LibraryRepository;
import com.pandadocs.api.repository.NotificationRepository;
import com.pandadocs.api.repository.OrderRepository;
import com.pandadocs.api.repository.TemplateRepository;
import com.pandadocs.api.repository.UserRepository;
import com.pandadocs.api.security.services.UserDetailsImpl;
import com.pandadocs.api.service.PayOSService;

import jakarta.persistence.EntityNotFoundException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api")
public class PurchaseController {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseController.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PayOSService payOSService;

    @Autowired
    private PayOSConfig payOSConfig;

    @PostMapping("/purchases")
    @Transactional
    public ResponseEntity<?> createPurchase(@RequestBody PurchaseRequest purchaseRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            User currentUser = userRepository.findByIdWithRoles(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Template templateToPurchase = templateRepository.findById(purchaseRequest.getTemplateId())
                    .orElseThrow(() -> new EntityNotFoundException("Template not found"));

            boolean alreadyOwned = libraryRepository.existsByUserAndTemplate(currentUser, templateToPurchase);
            if (alreadyOwned) {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: You already own this template."));
            }

            if (templateToPurchase.getPrice() == null || templateToPurchase.getPrice() == 0) {
                Library library = new Library();
                library.setUser(currentUser);
                library.setTemplate(templateToPurchase);
                library.setAcquiredAt(Instant.now());
                libraryRepository.save(library);

                Order order = new Order();
                order.setUser(currentUser);
                order.setCreatedAt(Instant.now());
                order.setTotalAmount(0.0);
                order.setStatus("COMPLETED");
                order.setPaymentStatus(PaymentStatus.PAID);

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setTemplate(templateToPurchase);
                orderItem.setPrice(0.0);
                order.getOrderItems().add(orderItem);
                orderRepository.save(order);

                PurchaseResponse response = new PurchaseResponse(
                    order.getId(),
                    null,
                    "Template miễn phí đã được thêm vào thư viện của bạn"
                );
                return ResponseEntity.ok(response);
            }

            Order order = new Order();
            order.setUser(currentUser);
            order.setCreatedAt(Instant.now());
            order.setTotalAmount(templateToPurchase.getPrice());
            order.setStatus("PENDING");
            order.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setTemplate(templateToPurchase);
            orderItem.setPrice(templateToPurchase.getPrice());
            order.getOrderItems().add(orderItem);

            order = orderRepository.save(order);

            String templateTitle = templateToPurchase.getTitle();
            String description = templateTitle.length() <= 22
                    ? templateTitle
                    : templateTitle.substring(0, 22) + "...";
            String returnUrl = payOSConfig.getReturnUrl();
            String cancelUrl = payOSConfig.getCancelUrl();

            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name(templateToPurchase.getTitle())
                    .quantity(1)
                    .price(templateToPurchase.getPrice().longValue())
                    .build();

            CreatePaymentLinkResponse paymentResponse = payOSService.createPaymentLink(
                    order.getId(),
                    templateToPurchase.getPrice().longValue(),
                    description,
                    Arrays.asList(item),
                    cancelUrl,
                    returnUrl
            );

            if (paymentResponse != null) {
                order.setPaymentId(paymentResponse.getPaymentLinkId());
                order.setPaymentUrl(paymentResponse.getCheckoutUrl());
                orderRepository.save(order);

                PurchaseResponse response = new PurchaseResponse(
                    order.getId(),
                    paymentResponse.getCheckoutUrl(),
                    "Vui lòng thanh toán để hoàn tất mua template"
                );
                return ResponseEntity.ok(response);
            }

            orderRepository.delete(order);
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Failed to create payment link"));
        } catch (Exception e) {
            logger.error("Payment creation failed for template {}", purchaseRequest.getTemplateId(), e);
            return ResponseEntity.badRequest().body(new MessageResponse("Payment creation failed"));
        }
    }
}
