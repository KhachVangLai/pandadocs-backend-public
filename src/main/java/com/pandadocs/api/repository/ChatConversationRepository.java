package com.pandadocs.api.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.pandadocs.api.model.ChatConversation;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
  Optional<ChatConversation> findBySessionId(String sessionId);

  Boolean existsBySessionId(String sessionId);

  @Query("SELECT c FROM ChatConversation c WHERE c.user.id = :userId ORDER BY c.lastActivityAt DESC")
  List<ChatConversation> findByUserIdOrderByLastActivityAtDesc(@Param("userId") Long userId);

  @Query("SELECT c FROM ChatConversation c LEFT JOIN FETCH c.user WHERE c.sessionId = :sessionId AND c.user.id = :userId")
  Optional<ChatConversation> findBySessionIdAndUserId(@Param("sessionId") String sessionId, @Param("userId") Long userId);

  @Query("SELECT c FROM ChatConversation c WHERE c.lastActivityAt < :cutoffTime")
  List<ChatConversation> findOlderThan(@Param("cutoffTime") Instant cutoffTime);

  long countByUserId(Long userId);

  void deleteByLastActivityAtBefore(Instant cutoffTime);
}
