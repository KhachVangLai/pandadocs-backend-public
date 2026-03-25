package com.pandadocs.api.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pandadocs.api.dto.ChatMessageRequest;
import com.pandadocs.api.dto.ChatMessageResponse;
import com.pandadocs.api.dto.ChatSessionResponse;
import com.pandadocs.api.dto.MessageResponse;
import com.pandadocs.api.dto.PurchaseActionRequest;
import com.pandadocs.api.exception.ChatSessionNotFoundException;
import com.pandadocs.api.exception.RateLimitExceededException;
import com.pandadocs.api.security.services.UserDetailsImpl;
import com.pandadocs.api.service.ChatService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for AI Chatbox endpoints
 * All endpoints require authentication (USER role)
 */
@RestController
@RequestMapping("/api/chat")
@PreAuthorize("hasRole('USER')")
@Slf4j
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * Send a message to AI chatbox and get response
     *
     * POST /api/chat/message
     * Body: { "sessionId": "optional", "message": "user message" }
     * Response: { "sessionId", "message", "templates", "actionButtons", "conversationTitle" }
     */
    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(
            @Valid @RequestBody ChatMessageRequest request,
            Authentication authentication) {

        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            Long userId = userDetails.getId();

            log.info("User {} sending chat message: sessionId={}, message={}",
                    userId, request.getSessionId(), request.getMessage());

            ChatMessageResponse response = chatService.processMessage(
                    userId,
                    request.getSessionId(),
                    request.getMessage()
            );

            return ResponseEntity.ok(response);

        } catch (RateLimitExceededException e) {
            log.warn("Rate limit exceeded: {}", e.getMessage());
            return ResponseEntity.status(429).body(Map.of(
                    "error", "RATE_LIMIT_EXCEEDED",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "INTERNAL_ERROR",
                    "message", "Đã xảy ra lỗi. Vui lòng thử lại sau."
            ));
        }
    }

    /**
     * Get current chat session
     *
     * GET /api/chat/session?sessionId=xxx
     * Response: { "sessionId", "conversationTitle", "messageCount", "messages" }
     */
    @GetMapping("/session")
    public ResponseEntity<?> getSession(
            @RequestParam String sessionId,
            Authentication authentication) {

        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            Long userId = userDetails.getId();

            log.debug("User {} retrieving session: {}", userId, sessionId);

            ChatSessionResponse response = chatService.getSession(userId, sessionId);
            return ResponseEntity.ok(response);

        } catch (ChatSessionNotFoundException e) {
            log.warn("Session not found: {}", sessionId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving session", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "INTERNAL_ERROR",
                    "message", "Đã xảy ra lỗi. Vui lòng thử lại sau."
            ));
        }
    }

    /**
     * Clear/delete a chat session
     *
     * DELETE /api/chat/session?sessionId=xxx
     * Response: { "message": "Session cleared successfully" }
     */
    @DeleteMapping("/session")
    public ResponseEntity<?> clearSession(
            @RequestParam String sessionId,
            Authentication authentication) {

        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            Long userId = userDetails.getId();

            log.info("User {} clearing session: {}", userId, sessionId);

            chatService.clearSession(userId, sessionId);
            return ResponseEntity.ok(new MessageResponse("Session cleared successfully"));

        } catch (ChatSessionNotFoundException e) {
            log.warn("Session not found: {}", sessionId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error clearing session", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "INTERNAL_ERROR",
                    "message", "Đã xảy ra lỗi. Vui lòng thử lại sau."
            ));
        }
    }

    /**
     * Handle purchase action from chat (Buy/Add/View buttons)
     *
     * POST /api/chat/purchase-action
     * Body: { "sessionId", "action": "BUY_NOW", "templateId": 123 }
     * Response: Depends on action type
     */
    @PostMapping("/purchase-action")
    public ResponseEntity<?> handlePurchaseAction(
            @Valid @RequestBody PurchaseActionRequest request,
            Authentication authentication) {

        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            Long userId = userDetails.getId();

            log.info("User {} performing purchase action: {} for template {}",
                    userId, request.getAction(), request.getTemplateId());

            return chatService.handlePurchaseAction(userId, request);

        } catch (ChatSessionNotFoundException e) {
            log.warn("Session not found: {}", request.getSessionId());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error handling purchase action", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "INTERNAL_ERROR",
                    "message", "Đã xảy ra lỗi. Vui lòng thử lại sau."
            ));
        }
    }
}
