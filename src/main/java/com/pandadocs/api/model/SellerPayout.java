package com.pandadocs.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "seller_payouts")
@Getter
@Setter
@NoArgsConstructor
public class SellerPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private Double proposedPrice; // Giá seller đề xuất

    @Column(nullable = false)
    private Double agreedPrice; // Giá admin đồng ý trả

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PayoutStatus status; // PENDING, PAID, REJECTED

    @Column(columnDefinition = "TEXT")
    private String adminNote; // Ghi chú của admin

    private Instant paidAt; // Thời điểm admin confirm đã trả

    @Column(nullable = false)
    private Instant createdAt; // Thời điểm tạo payout record
}
