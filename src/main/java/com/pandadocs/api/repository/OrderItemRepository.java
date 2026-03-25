package com.pandadocs.api.repository;

import com.pandadocs.api.model.OrderItem;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    // Đếm số lượt bán của một seller (tác giả)
    @Query("SELECT count(oi) FROM OrderItem oi WHERE oi.template.author.id = :authorId")
    long countSalesByAuthorId(@Param("authorId") Long authorId);

    // Tính tổng doanh thu của một seller (tác giả)
    @Query("SELECT COALESCE(SUM(oi.price), 0.0) FROM OrderItem oi WHERE oi.template.author.id = :authorId")
    double sumEarningsByAuthorId(@Param("authorId") Long authorId);

    // Tính tổng doanh thu trong một khoảng thời gian
    @Query("SELECT COALESCE(SUM(oi.price), 0.0) FROM OrderItem oi WHERE oi.order.createdAt >= :startDate")
    double sumRevenueSince(@Param("startDate") Instant startDate);

    // Tính tổng doanh thu của tất cả các đơn hàng trong một khoảng thời gian
    @Query("SELECT COALESCE(SUM(oi.price), 0.0) FROM OrderItem oi WHERE oi.order.createdAt BETWEEN :startDate AND :endDate")
    double sumTotalRevenueForPeriod(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}