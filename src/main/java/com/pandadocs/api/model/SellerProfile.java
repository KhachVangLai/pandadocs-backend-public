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
    private Long id; // Dùng chung ID với User

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // Đánh dấu rằng trường này là cả Khóa chính và Khóa ngoại
    @JoinColumn(name = "id")
    private User user;

    private String businessName;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Thông tin ngân hàng để admin chuyển tiền (BẮT BUỘC để nhận thanh toán)
    private String bankName;

    private String bankAccountNumber;

    private String bankAccountHolderName;
}