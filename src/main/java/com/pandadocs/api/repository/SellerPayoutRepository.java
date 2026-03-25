package com.pandadocs.api.repository;

import com.pandadocs.api.model.PayoutStatus;
import com.pandadocs.api.model.SellerPayout;
import com.pandadocs.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerPayoutRepository extends JpaRepository<SellerPayout, Long> {

    // Tìm tất cả payout theo status
    List<SellerPayout> findByStatus(PayoutStatus status);

    // Tìm payout theo seller
    List<SellerPayout> findBySeller(User seller);

    // Tìm payout theo seller và status
    List<SellerPayout> findBySellerAndStatus(User seller, PayoutStatus status);

    // Tìm payout theo templateId
    Optional<SellerPayout> findByTemplateId(Long templateId);

    // Tính tổng earnings của seller (chỉ tính PAID)
    @Query("SELECT COALESCE(SUM(sp.agreedPrice), 0.0) FROM SellerPayout sp WHERE sp.seller = :seller AND sp.status = 'PAID'")
    Double calculateTotalEarnings(@Param("seller") User seller);

    // Tính tổng pending payout của seller
    @Query("SELECT COALESCE(SUM(sp.agreedPrice), 0.0) FROM SellerPayout sp WHERE sp.seller = :seller AND sp.status = 'PENDING'")
    Double calculatePendingEarnings(@Param("seller") User seller);

    // Lấy danh sách payout pending (cho admin)
    @Query("SELECT sp FROM SellerPayout sp WHERE sp.status = 'PENDING' ORDER BY sp.createdAt DESC")
    List<SellerPayout> findAllPendingPayouts();

    // Kiểm tra xem template đã có payout chưa
    boolean existsByTemplateId(Long templateId);

    // Xóa tất cả payouts của một template
    @Modifying
    @Transactional
    void deleteByTemplateId(Long templateId);
}
