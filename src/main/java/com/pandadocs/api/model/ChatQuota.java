package com.pandadocs.api.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_quota",
       indexes = {
           @Index(name = "idx_chat_quota_user_id", columnList = "user_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatQuota {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "hourly_count", nullable = false)
  @Builder.Default
  private Integer hourlyCount = 0;

  @Column(name = "daily_count", nullable = false)
  @Builder.Default
  private Integer dailyCount = 0;

  @Column(name = "last_hourly_reset", nullable = false)
  @Builder.Default
  private Instant lastHourlyReset = Instant.now();

  @Column(name = "last_daily_reset", nullable = false)
  @Builder.Default
  private Instant lastDailyReset = Instant.now();

  @Column(name = "created_at", nullable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();

  public ChatQuota(User user) {
    this.user = user;
    this.hourlyCount = 0;
    this.dailyCount = 0;
    this.lastHourlyReset = Instant.now();
    this.lastDailyReset = Instant.now();
    this.createdAt = Instant.now();
  }

  /**
   * Checks if hourly quota needs to be reset (more than 1 hour since last reset)
   * @return true if needs reset
   */
  public boolean needsHourlyReset() {
    Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
    return lastHourlyReset.isBefore(oneHourAgo);
  }

  /**
   * Checks if daily quota needs to be reset (more than 1 day since last reset)
   * @return true if needs reset
   */
  public boolean needsDailyReset() {
    Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
    return lastDailyReset.isBefore(oneDayAgo);
  }

  /**
   * Resets hourly quota
   */
  public void resetHourly() {
    this.hourlyCount = 0;
    this.lastHourlyReset = Instant.now();
  }

  /**
   * Resets daily quota
   */
  public void resetDaily() {
    this.dailyCount = 0;
    this.lastDailyReset = Instant.now();
  }

  /**
   * Increments both hourly and daily counts
   */
  public void increment() {
    this.hourlyCount++;
    this.dailyCount++;
  }
}
