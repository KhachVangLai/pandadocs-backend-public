package com.pandadocs.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pandadocs.api.config.GeminiConfig;
import com.pandadocs.api.dto.ChatActionButtonDTO;
import com.pandadocs.api.dto.ChatMessageResponse;
import com.pandadocs.api.dto.ChatSessionResponse;
import com.pandadocs.api.dto.PurchaseActionRequest;
import com.pandadocs.api.dto.TemplateCardDTO;
import com.pandadocs.api.exception.ChatSessionNotFoundException;
import com.pandadocs.api.model.ChatConversation;
import com.pandadocs.api.model.User;
import com.pandadocs.api.model.chat.ChatMessage;
import com.pandadocs.api.model.chat.ChatSession;
import com.pandadocs.api.repository.ChatConversationRepository;
import com.pandadocs.api.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Main orchestration service for AI Chatbox
 * Coordinates between GeminiService, TemplateSearchService, SessionManager, etc.
 */
@Service
@Slf4j
public class ChatService {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private TemplateSearchService templateSearchService;

    @Autowired
    private ChatSessionManager sessionManager;

    @Autowired
    private ChatRateLimiter rateLimiter;

    @Autowired
    private ChatConversationRepository conversationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GeminiConfig geminiConfig;

    /**
     * Processes a user message and returns AI response
     *
     * @param userId     User ID
     * @param sessionId  Session ID (optional, will create if null)
     * @param userMessage User's message
     * @return ChatMessageResponse
     */
    @Transactional
    public ChatMessageResponse processMessage(Long userId, String sessionId, String userMessage) {
        rateLimiter.checkAndIncrementQuota(userId);

        ChatSession session;
        if (sessionId != null && !sessionId.isBlank()) {
            session = sessionManager.getSessionForUser(sessionId, userId)
                    .orElseGet(() -> sessionManager.createOrGetSession(userId));
        } else {
            session = sessionManager.createOrGetSession(userId);
        }

        ChatMessage userChatMessage = ChatMessage.builder()
                .role(ChatMessage.MessageRole.USER)
                .content(userMessage)
                .build();
        session.addMessage(userChatMessage);

        List<TemplateCardDTO> templates = detectAndSearchTemplates(userMessage);

        if (templates.isEmpty()) {
            log.info("No templates found from search, showing popular templates");
            templates = templateSearchService.getPopularTemplates(10);
        }

        log.info("Passing {} templates to Gemini for context", templates.size());

        String aiResponse;
        try {
            aiResponse = geminiService.generateResponseWithContext(
                    userMessage,
                    session.getMessages(),
                    templates
            );
            log.debug("Gemini response: {}", aiResponse);
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            aiResponse = generateFallbackResponse(templates);
        }

        ChatMessage aiChatMessage = ChatMessage.builder()
                .role(ChatMessage.MessageRole.ASSISTANT)
                .content(aiResponse)
                .build();
        session.addMessage(aiChatMessage);

        if (session.getConversationTitle() == null && geminiConfig.isAutoTitleEnabled()) {
            try {
                String title = geminiService.generateConversationTitle(userMessage);
                session.setConversationTitle(title);
                log.debug("Generated conversation title: {}", title);
            } catch (Exception e) {
                log.error("Error generating conversation title", e);
            }
        }

        List<ChatActionButtonDTO> actionButtons = detectPurchaseIntent(aiResponse, templates);

        saveConversationMetadata(userId, session);

        return ChatMessageResponse.builder()
                .sessionId(session.getSessionId())
                .message(aiResponse)
                .templates(templates)
                .actionButtons(actionButtons)
                .conversationTitle(session.getConversationTitle())
                .build();
    }

    /**
     * Gets the current session for a user
     *
     * @param userId    User ID
     * @param sessionId Session ID
     * @return ChatSessionResponse
     */
    public ChatSessionResponse getSession(Long userId, String sessionId) {
        ChatSession session = sessionManager.getSessionForUser(sessionId, userId)
                .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));

        List<ChatSessionResponse.ChatMessageDTO> messageDTOs = session.getMessages().stream()
                .map(msg -> ChatSessionResponse.ChatMessageDTO.builder()
                        .role(msg.getRole().toString())
                        .content(msg.getContent())
                        .timestamp(msg.getTimestamp())
                        .build())
                .collect(Collectors.toList());

        return ChatSessionResponse.builder()
                .sessionId(session.getSessionId())
                .conversationTitle(session.getConversationTitle())
                .messageCount(session.getMessageCount())
                .createdAt(session.getCreatedAt())
                .messages(messageDTOs)
                .build();
    }

    /**
     * Clears a session
     *
     * @param userId    User ID
     * @param sessionId Session ID
     */
    public void clearSession(Long userId, String sessionId) {
        sessionManager.getSessionForUser(sessionId, userId)
                .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));

        sessionManager.deleteSession(sessionId);
        log.info("Cleared session {} for user {}", sessionId, userId);
    }

    /**
     * Handles purchase action from chat (BUY_NOW, ADD_TO_LIBRARY, etc.)
     *
     * @param userId  User ID
     * @param request Purchase action request
     * @return ResponseEntity with appropriate action
     */
    public ResponseEntity<?> handlePurchaseAction(Long userId, PurchaseActionRequest request) {
        sessionManager.getSessionForUser(request.getSessionId(), userId)
                .orElseThrow(() -> new ChatSessionNotFoundException(request.getSessionId()));

        TemplateCardDTO template = templateSearchService.getTemplateById(request.getTemplateId());
        if (template == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Template not found"));
        }

        switch (request.getAction()) {
            case BUY_NOW:
                return ResponseEntity.ok(Map.of(
                        "action", "REDIRECT_TO_PURCHASE",
                        "templateId", request.getTemplateId(),
                        "endpoint", "/api/purchases",
                        "message", "Redirecting to checkout..."
                ));

            case ADD_TO_LIBRARY:
                if (template.getPrice() > 0) {
                    return ResponseEntity.badRequest().body(Map.of("error", "This is not a free template"));
                }
                return ResponseEntity.ok(Map.of(
                        "action", "ADD_TO_LIBRARY",
                        "templateId", request.getTemplateId(),
                        "endpoint", "/api/users/library/" + request.getTemplateId(),
                        "message", "Adding to library..."
                ));

            case VIEW_DETAILS:
                return ResponseEntity.ok(Map.of(
                        "action", "VIEW_DETAILS",
                        "templateId", request.getTemplateId(),
                        "url", "/templates/" + request.getTemplateId(),
                        "message", "Opening template details..."
                ));

            case CANCEL:
                return ResponseEntity.ok(Map.of("message", "Action cancelled"));

            default:
                return ResponseEntity.badRequest().body(Map.of("error", "Unknown action"));
        }
    }

    /**
     * Detects search intent in user message and searches for templates
     *
     * @param userMessage User's message
     * @return List of templates (empty if no search intent)
     */
    private List<TemplateCardDTO> detectAndSearchTemplates(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();

        boolean isSearchIntent =
                lowerMessage.contains("tìm") ||
                lowerMessage.contains("search") ||
                lowerMessage.contains("template") ||
                lowerMessage.contains("mẫu") ||
                lowerMessage.contains("cần") ||
                lowerMessage.contains("muốn") ||
                lowerMessage.contains("giúp tôi");

        if (!isSearchIntent) {
            return new ArrayList<>();
        }

        Double maxPrice = extractMaxPrice(lowerMessage);

        String category = extractCategory(lowerMessage);

        return templateSearchService.searchTemplates(userMessage, category, maxPrice, 10);
    }

    /**
     * Detects purchase intent and generates action buttons
     *
     * @param aiResponse AI's response
     * @param templates  Templates in the context
     * @return List of action buttons
     */
    private List<ChatActionButtonDTO> detectPurchaseIntent(String aiResponse, List<TemplateCardDTO> templates) {
        String lowerResponse = aiResponse.toLowerCase();

        boolean isPurchaseConfirm =
                lowerResponse.contains("bạn có muốn") ||
                lowerResponse.contains("bạn muốn mua") ||
                lowerResponse.contains("bạn có muốn thêm") ||
                lowerResponse.contains("bạn có muốn xem");

        if (!isPurchaseConfirm || templates == null || templates.isEmpty()) {
            return null;
        }

        TemplateCardDTO firstTemplate = templates.get(0);
        List<ChatActionButtonDTO> buttons = new ArrayList<>();

        if (firstTemplate.getPrice() == 0) {
            buttons.add(ChatActionButtonDTO.builder()
                    .type(ChatActionButtonDTO.ActionType.ADD_TO_LIBRARY)
                    .label("Thêm vào library")
                    .templateId(firstTemplate.getId())
                    .build());
        } else {
            buttons.add(ChatActionButtonDTO.builder()
                    .type(ChatActionButtonDTO.ActionType.BUY_NOW)
                    .label("Mua ngay (" + String.format("%.0f", firstTemplate.getPrice()) + "đ)")
                    .templateId(firstTemplate.getId())
                    .build());
        }

        buttons.add(ChatActionButtonDTO.builder()
                .type(ChatActionButtonDTO.ActionType.VIEW_DETAILS)
                .label("Xem chi tiết")
                .templateId(firstTemplate.getId())
                .build());

        buttons.add(ChatActionButtonDTO.builder()
                .type(ChatActionButtonDTO.ActionType.CANCEL)
                .label("Hủy")
                .templateId(firstTemplate.getId())
                .build());

        return buttons;
    }

    /**
     * Extracts maximum price from user message
     */
    private Double extractMaxPrice(String message) {
        // Matches phrases such as "under 100k", "max 50000", or "< 100000".
        Pattern pattern = Pattern.compile("(dưới|max|tối đa|<)\\s*(\\d+)(k|000)?");
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            String numberStr = matcher.group(2);
            String unit = matcher.group(3);

            double number = Double.parseDouble(numberStr);
            if (unit != null && unit.equals("k")) {
                number *= 1000;
            }
            return number;
        }

        return null;
    }

    /**
     * Extracts category from user message
     */
    private String extractCategory(String message) {
        String lower = message.toLowerCase();

        if (lower.contains("resume") || lower.contains("cv")) return "Resume/CV";
        if (lower.contains("presentation") || lower.contains("slide")) return "Presentation";
        if (lower.contains("report") || lower.contains("báo cáo")) return "Report";
        if (lower.contains("invoice") || lower.contains("hóa đơn")) return "Invoice";
        if (lower.contains("contract") || lower.contains("hợp đồng")) return "Contract";
        if (lower.contains("thesis") || lower.contains("luận văn")) return "Thesis";

        return null;
    }

    /**
     * Generates a fallback response when Gemini API fails
     * Returns a simple text response listing available templates
     *
     * @param templates List of templates to include in response
     * @return Fallback message in Vietnamese
     */
    private String generateFallbackResponse(List<TemplateCardDTO> templates) {
        if (templates == null || templates.isEmpty()) {
            return "Xin lỗi, hiện tại không tìm thấy template nào phù hợp. Bạn có thể thử tìm kiếm với từ khóa khác hoặc xem các template phổ biến của chúng tôi.";
        }

        StringBuilder response = new StringBuilder();
        response.append("Tôi tìm thấy ");
        response.append(templates.size());
        response.append(" template");
        if (templates.size() > 1) {
            response.append("s");
        }
        response.append(" có thể phù hợp với bạn:\n\n");

        // List up to 5 templates
        int displayCount = Math.min(templates.size(), 5);
        for (int i = 0; i < displayCount; i++) {
            TemplateCardDTO t = templates.get(i);
            response.append(String.format("%d. **%s**\n", i + 1, t.getTitle()));

            if (t.getDescription() != null && !t.getDescription().isEmpty()) {
                String desc = t.getDescription();
                if (desc.length() > 100) {
                    desc = desc.substring(0, 97) + "...";
                }
                response.append("   ").append(desc).append("\n");
            }

            response.append("   Giá: ");
            if (t.getPrice() == 0) {
                response.append("**MIỄN PHÍ**");
            } else {
                response.append(String.format("**%.0f VNĐ**", t.getPrice()));
            }
            response.append("\n");

            if (t.getCategory() != null) {
                response.append("   Danh mục: ").append(t.getCategory()).append("\n");
            }

            response.append("\n");
        }

        response.append("Bạn có muốn xem chi tiết template nào không?");

        return response.toString();
    }

    /**
     * Saves conversation metadata to database
     */
    private void saveConversationMetadata(Long userId, ChatSession session) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return;

            conversationRepository.findBySessionId(session.getSessionId())
                    .ifPresentOrElse(
                            conversation -> {
                                conversation.setTitle(session.getConversationTitle());
                                conversation.setMessageCount(session.getMessageCount());
                                conversation.updateActivity();
                                conversationRepository.save(conversation);
                            },
                            () -> {
                                ChatConversation conversation = ChatConversation.builder()
                                        .user(user)
                                        .sessionId(session.getSessionId())
                                        .title(session.getConversationTitle())
                                        .messageCount(session.getMessageCount())
                                        .build();
                                conversationRepository.save(conversation);
                            }
                    );
        } catch (Exception e) {
            log.error("Error saving conversation metadata", e);
        }
    }
}
