package com.pandadocs.api.repository;

import com.pandadocs.api.model.OrderItem;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    @Query("SELECT count(oi) FROM OrderItem oi WHERE oi.template.author.id = :authorId")
    long countSalesByAuthorId(@Param("authorId") Long authorId);

    @Query("SELECT COALESCE(SUM(oi.price), 0.0) FROM OrderItem oi WHERE oi.template.author.id = :authorId")
    double sumEarningsByAuthorId(@Param("authorId") Long authorId);

    @Query("SELECT COALESCE(SUM(oi.price), 0.0) FROM OrderItem oi WHERE oi.order.createdAt >= :startDate")
    double sumRevenueSince(@Param("startDate") Instant startDate);

    @Query("SELECT COALESCE(SUM(oi.price), 0.0) FROM OrderItem oi WHERE oi.order.createdAt BETWEEN :startDate AND :endDate")
    double sumTotalRevenueForPeriod(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}
