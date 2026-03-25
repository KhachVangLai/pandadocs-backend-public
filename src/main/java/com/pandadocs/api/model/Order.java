package com.pandadocs.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(length = 50)
    private String status; // Deprecated: Dùng paymentStatus thay thế

    // PayOS payment fields
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentStatus paymentStatus; // PENDING_PAYMENT, PAID, FAILED, CANCELLED

    private String paymentId; // PayOS orderCode/transaction ID

    @Column(columnDefinition = "TEXT")
    private String paymentUrl; // Link thanh toán PayOS

    private Instant paidAt; // Thời điểm thanh toán thành công

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderItem> orderItems = new HashSet<>();
}