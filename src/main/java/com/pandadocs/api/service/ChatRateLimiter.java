package com.pandadocs.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pandadocs.api.config.GeminiConfig;
import com.pandadocs.api.exception.RateLimitExceededException;
import com.pandadocs.api.model.ChatQuota;
import com.pandadocs.api.model.User;
import com.pandadocs.api.repository.ChatQuotaRepository;
import com.pandadocs.api.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing chat rate limiting
 * Enforces hourly and daily message limits per user
 */
@Service
@Slf4j
public class ChatRateLimiter {

    @Autowired
    private ChatQuotaRepository chatQuotaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GeminiConfig geminiConfig;

    /**
     * Checks if user can send a message, and increments quota if allowed
     * Throws RateLimitExceededException if limit exceeded
     *
     * @param userId User ID
     * @throws RateLimitExceededException if rate limit exceeded
     */
    @Transactional
    public void checkAndIncrementQuota(Long userId) {
        ChatQuota quota = chatQuotaRepository.findByUserId(userId)
                .orElseGet(() -> createNewQuota(userId));

        // Reset quotas if needed
        boolean resetHourly = quota.needsHourlyReset();
        boolean resetDaily = quota.needsDailyReset();

        if (resetHourly) {
            quota.resetHourly();
            log.debug("Reset hourly quota for user {}", userId);
        }

        if (resetDaily) {
            quota.resetDaily();
            log.debug("Reset daily quota for user {}", userId);
        }

        // Check limits
        int hourlyLimit = geminiConfig.getMessagesPerHour();
        int dailyLimit = geminiConfig.getMessagesPerDay();

        if (quota.getHourlyCount() >= hourlyLimit) {
            log.warn("User {} exceeded hourly rate limit ({}/{})", userId, quota.getHourlyCount(), hourlyLimit);
            throw new RateLimitExceededException(
                String.format("Bạn đã vượt quá giới hạn %d tin nhắn mỗi giờ. Vui lòng thử lại sau.", hourlyLimit)
            );
        }

        if (quota.getDailyCount() >= dailyLimit) {
            log.warn("User {} exceeded daily rate limit ({}/{})", userId, quota.getDailyCount(), dailyLimit);
            throw new RateLimitExceededException(
                String.format("Bạn đã vượt quá giới hạn %d tin nhắn mỗi ngày. Vui lòng thử lại vào ngày mai.", dailyLimit)
            );
        }

        // Increment quota
        quota.increment();
        chatQuotaRepository.save(quota);

        log.debug("User {} quota: {}/{} hourly, {}/{} daily",
                userId,
                quota.getHourlyCount(), hourlyLimit,
                quota.getDailyCount(), dailyLimit);
    }

    /**
     * Gets the current quota for a user (for informational purposes)
     *
     * @param userId User ID
     * @return ChatQuota
     */
    public ChatQuota getQuota(Long userId) {
        return chatQuotaRepository.findByUserId(userId)
                .orElseGet(() -> createNewQuota(userId));
    }

    /**
     * Resets quota for a user (admin function)
     *
     * @param userId User ID
     */
    @Transactional
    public void resetQuota(Long userId) {
        ChatQuota quota = chatQuotaRepository.findByUserId(userId)
                .orElse(null);

        if (quota != null) {
            quota.resetHourly();
            quota.resetDaily();
            chatQuotaRepository.save(quota);
            log.info("Admin reset quota for user {}", userId);
        }
    }

    /**
     * Creates a new quota record for a user
     *
     * @param userId User ID
     * @return ChatQuota
     */
    private ChatQuota createNewQuota(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        ChatQuota quota = new ChatQuota(user);
        chatQuotaRepository.save(quota);
        log.info("Created new quota for user {}", userId);
        return quota;
    }
}
