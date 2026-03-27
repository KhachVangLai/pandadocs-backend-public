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

    List<SellerPayout> findByStatus(PayoutStatus status);

    List<SellerPayout> findBySeller(User seller);

    List<SellerPayout> findBySellerAndStatus(User seller, PayoutStatus status);

    Optional<SellerPayout> findByTemplateId(Long templateId);

    @Query("SELECT COALESCE(SUM(sp.agreedPrice), 0.0) FROM SellerPayout sp WHERE sp.seller = :seller AND sp.status = 'PAID'")
    Double calculateTotalEarnings(@Param("seller") User seller);

    @Query("SELECT COALESCE(SUM(sp.agreedPrice), 0.0) FROM SellerPayout sp WHERE sp.seller = :seller AND sp.status = 'PENDING'")
    Double calculatePendingEarnings(@Param("seller") User seller);

    @Query("SELECT sp FROM SellerPayout sp WHERE sp.status = 'PENDING' ORDER BY sp.createdAt DESC")
    List<SellerPayout> findAllPendingPayouts();

    boolean existsByTemplateId(Long templateId);

    @Modifying
    @Transactional
    void deleteByTemplateId(Long templateId);
}
