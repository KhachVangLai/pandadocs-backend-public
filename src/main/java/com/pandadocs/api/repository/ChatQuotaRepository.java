package com.pandadocs.api.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.pandadocs.api.model.ChatQuota;

@Repository
public interface ChatQuotaRepository extends JpaRepository<ChatQuota, Long> {
  @Query("SELECT q FROM ChatQuota q WHERE q.user.id = :userId")
  Optional<ChatQuota> findByUserId(@Param("userId") Long userId);

  @Query("SELECT CASE WHEN COUNT(q) > 0 THEN true ELSE false END FROM ChatQuota q WHERE q.user.id = :userId")
  Boolean existsByUserId(@Param("userId") Long userId);

  void deleteByUserId(Long userId);
}
