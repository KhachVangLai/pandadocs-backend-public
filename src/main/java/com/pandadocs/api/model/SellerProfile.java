package com.pandadocs.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seller_profiles")
@Getter
@Setter
@NoArgsConstructor
public class SellerProfile {
    @Id
    private Long id; // Shares the same ID as User.

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // Reuses the user ID as both the primary key and foreign key.
    @JoinColumn(name = "id")
    private User user;

    private String businessName;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Required bank details for seller payouts.
    private String bankName;

    private String bankAccountNumber;

    private String bankAccountHolderName;
}
