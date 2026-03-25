package com.pandadocs.api.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.pandadocs.api.model.chat.ChatSession;

import lombok.extern.slf4j.Slf4j;

/**
 * Manages in-memory chat sessions
 * Sessions expire after 30 minutes of inactivity
 */
@Service
@Slf4j
public class ChatSessionManager {

    // In-memory storage: sessionId -> ChatSession
    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();

    // User's active sessions: userId -> sessionId
    private final Map<Long, String> userActiveSessions = new ConcurrentHashMap<>();

    /**
     * Creates a new chat session for a user
     * If user already has an active session, returns that session
     *
     * @param userId User ID
     * @return ChatSession
     */
    public ChatSession createOrGetSession(Long userId) {
        // Check if user already has an active session
        String existingSessionId = userActiveSessions.get(userId);
        if (existingSessionId != null) {
            ChatSession existingSession = sessions.get(existingSessionId);
            if (existingSession != null && !existingSession.isExpired()) {
                log.debug("Returning existing session {} for user {}", existingSessionId, userId);
                return existingSession;
            } else {
                // Session expired, remove it
                sessions.remove(existingSessionId);
                userActiveSessions.remove(userId);
            }
        }

        // Create new session
        String sessionId = UUID.randomUUID().toString();
        ChatSession session = ChatSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .build();

        sessions.put(sessionId, session);
        userActiveSessions.put(userId, sessionId);

        log.info("Created new chat session {} for user {}", sessionId, userId);
        return session;
    }

    /**
     * Gets a session by session ID
     *
     * @param sessionId Session ID
     * @return Optional<ChatSession>
     */
    public Optional<ChatSession> getSession(String sessionId) {
        ChatSession session = sessions.get(sessionId);
        if (session != null) {
            if (session.isExpired()) {
                // Remove expired session
                deleteSession(sessionId);
                return Optional.empty();
            }
            session.updateActivity();
            return Optional.of(session);
        }
        return Optional.empty();
    }

    /**
     * Gets a session and verifies it belongs to the user
     *
     * @param sessionId Session ID
     * @param userId    User ID
     * @return Optional<ChatSession>
     */
    public Optional<ChatSession> getSessionForUser(String sessionId, Long userId) {
        Optional<ChatSession> sessionOpt = getSession(sessionId);
        if (sessionOpt.isPresent()) {
            ChatSession session = sessionOpt.get();
            if (session.getUserId().equals(userId)) {
                return Optional.of(session);
            } else {
                log.warn("Session {} does not belong to user {}", sessionId, userId);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * Deletes a session
     *
     * @param sessionId Session ID
     */
    public void deleteSession(String sessionId) {
        ChatSession session = sessions.remove(sessionId);
        if (session != null) {
            userActiveSessions.remove(session.getUserId());
            log.info("Deleted session {}", sessionId);
        }
    }

    /**
     * Deletes all sessions for a user
     *
     * @param userId User ID
     */
    public void deleteUserSessions(Long userId) {
        String sessionId = userActiveSessions.remove(userId);
        if (sessionId != null) {
            sessions.remove(sessionId);
            log.info("Deleted all sessions for user {}", userId);
        }
    }

    /**
     * Gets the number of active sessions
     *
     * @return Session count
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Cleanup expired sessions every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredSessions() {
        int initialCount = sessions.size();
        Instant now = Instant.now();

        // Find and remove expired sessions
        sessions.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                userActiveSessions.remove(entry.getValue().getUserId());
                return true;
            }
            return false;
        });

        int removedCount = initialCount - sessions.size();
        if (removedCount > 0) {
            log.info("Cleaned up {} expired sessions. Active sessions: {}", removedCount, sessions.size());
        }
    }
}
