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
    private Double proposedPrice; // Price proposed by the seller.

    @Column(nullable = false)
    private Double agreedPrice; // Price approved by the admin.

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PayoutStatus status;

    @Column(columnDefinition = "TEXT")
    private String adminNote; // Internal note from the admin.

    private Instant paidAt; // Time when the payout was confirmed.

    @Column(nullable = false)
    private Instant createdAt; // Time when the payout record was created.
}
