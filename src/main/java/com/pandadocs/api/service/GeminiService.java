package com.pandadocs.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pandadocs.api.config.GeminiConfig;
import com.pandadocs.api.dto.TemplateCardDTO;
import com.pandadocs.api.exception.GeminiApiException;
import com.pandadocs.api.model.chat.ChatMessage;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service for interacting with Google Gemini API
 * Uses REST API approach (not SDK)
 * Implements RAG (Retrieval Augmented Generation) pattern
 */
@Service
@Slf4j
public class GeminiService {

    @Autowired
    private GeminiConfig geminiConfig;

    private final OkHttpClient httpClient;
    private final Gson gson;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // System prompt to restrict AI to only talk about templates
    private static final String SYSTEM_PROMPT = """
        Bạn là trợ lý AI của PandaDocs, một nền tảng mua bán template văn bản cho sinh viên Việt Nam.

        NHIỆM VỤ CỦA BẠN:
        - Giúp người dùng tìm kiếm và lựa chọn template phù hợp
        - Trả lời câu hỏi về templates có sẵn trên website
        - Đề xuất templates dựa trên nhu cầu của người dùng
        - Hướng dẫn người dùng mua hoặc thêm template vào library

        QUY TẮC BẮT BUỘC:
        1. CHỈ nói về các templates được cung cấp trong CONTEXT bên dưới
        2. KHÔNG trả lời câu hỏi ngoài lề (kiến thức chung, lời khuyên cá nhân, v.v.)
        3. Nếu người dùng hỏi ngoài chủ đề templates, lịch sự từ chối: "Tôi chỉ có thể giúp bạn tìm templates trên PandaDocs."
        4. Khi người dùng muốn mua/thêm template, hỏi XÁC NHẬN trước khi đề xuất action
        5. Luôn thân thiện, ngắn gọn, sử dụng tiếng Việt
        6. CHỈ đề cập templates có trong CONTEXT, KHÔNG bịa thêm templates không tồn tại

        Các loại templates phổ biến: Resume/CV, Presentation, Report, Invoice, Contract, Thesis, Essay, v.v.
        """;

    public GeminiService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Generates a response from Gemini AI WITH template context (RAG pattern)
     * This is the MAIN method that should be used by ChatService
     *
     * @param userMessage         User's message
     * @param conversationHistory Previous messages in the conversation
     * @param availableTemplates  Templates found by search (context for AI)
     * @return AI's response text
     */
    public String generateResponseWithContext(
            String userMessage,
            List<ChatMessage> conversationHistory,
            List<TemplateCardDTO> availableTemplates) {

        try {
            // Build context with template data
            String templateContext = buildTemplateContext(availableTemplates);

            // Build request body with context
            JsonObject requestBody = buildRequestBodyWithContext(
                    userMessage,
                    conversationHistory,
                    templateContext
            );

            // Call Gemini API
            return callGeminiAPI(requestBody);

        } catch (IOException e) {
            log.error("Error calling Gemini API", e);
            throw new GeminiApiException("Failed to call Gemini API", e);
        }
    }

    /**
     * Generates a response from Gemini AI WITHOUT template context
     * Use this for general questions only
     *
     * @param userMessage         User's message
     * @param conversationHistory Previous messages
     * @return AI's response text
     */
    public String generateResponse(String userMessage, List<ChatMessage> conversationHistory) {
        return generateResponseWithContext(userMessage, conversationHistory, new ArrayList<>());
    }

    /**
     * Generates a short conversation title based on the first message
     *
     * @param firstMessage First user message
     * @return Generated title (max 50 chars)
     */
    public String generateConversationTitle(String firstMessage) {
        try {
            String prompt = String.format(
                "Tạo tiêu đề ngắn gọn (tối đa 50 ký tự) cho cuộc trò chuyện này dựa trên câu hỏi đầu tiên: \"%s\". " +
                "Chỉ trả về tiêu đề, không giải thích.",
                firstMessage
            );

            JsonObject requestBody = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", prompt);
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);

            String title = callGeminiAPI(requestBody);

            // Trim to max 50 characters
            if (title.length() > 50) {
                title = title.substring(0, 47) + "...";
            }
            return title;

        } catch (Exception e) {
            log.error("Error generating conversation title", e);
            // Fallback: Use first 50 chars of first message
            return firstMessage.length() > 50
                ? firstMessage.substring(0, 47) + "..."
                : firstMessage;
        }
    }

    /**
     * Builds template context string for injection into prompt
     *
     * @param templates List of templates
     * @return Formatted context string
     */
    private String buildTemplateContext(List<TemplateCardDTO> templates) {
        if (templates == null || templates.isEmpty()) {
            return "Hiện tại không tìm thấy template nào phù hợp với yêu cầu.";
        }

        StringBuilder context = new StringBuilder();
        context.append("\n\n=== DANH SÁCH TEMPLATES CÓ SẴN ===\n\n");

        for (int i = 0; i < templates.size(); i++) {
            TemplateCardDTO template = templates.get(i);
            context.append(String.format("Template #%d:\n", i + 1));
            context.append(String.format("  - ID: %d\n", template.getId()));
            context.append(String.format("  - Tên: %s\n", template.getTitle()));

            if (template.getDescription() != null && !template.getDescription().isBlank()) {
                // Limit description to 200 chars
                String desc = template.getDescription();
                if (desc.length() > 200) {
                    desc = desc.substring(0, 197) + "...";
                }
                context.append(String.format("  - Mô tả: %s\n", desc));
            }

            context.append(String.format("  - Giá: %s\n",
                    template.getPrice() == 0 ? "MIỄN PHÍ" : String.format("%.0f VNĐ", template.getPrice())));

            if (template.getCategory() != null) {
                context.append(String.format("  - Danh mục: %s\n", template.getCategory()));
            }

            if (template.getRating() != null && template.getRating() > 0) {
                context.append(String.format("  - Đánh giá: %.1f/5 ⭐\n", template.getRating()));
            }

            if (template.getDownloads() != null && template.getDownloads() > 0) {
                context.append(String.format("  - Lượt tải: %d\n", template.getDownloads()));
            }

            context.append("\n");
        }

        context.append("=== KẾT THÚC DANH SÁCH ===\n\n");
        context.append("Hãy dựa vào danh sách trên để trả lời câu hỏi của người dùng. ");
        context.append("KHÔNG đề cập đến templates không có trong danh sách.\n");

        return context.toString();
    }

    /**
     * Sanitizes user input to prevent prompt injection
     *
     * @param input User input
     * @return Sanitized input
     */
    public String sanitizeInput(String input) {
        if (input == null) return "";

        // Remove potential prompt injection patterns
        String sanitized = input
                .replaceAll("(?i)ignore\\s+previous\\s+instructions", "")
                .replaceAll("(?i)ignore\\s+all\\s+previous", "")
                .replaceAll("(?i)system:", "")
                .replaceAll("(?i)assistant:", "")
                .replaceAll("(?i)user:", "");

        // Trim and limit length
        sanitized = sanitized.trim();
        if (sanitized.length() > 500) {
            sanitized = sanitized.substring(0, 500);
        }

        return sanitized;
    }

    /**
     * Builds the request body for Gemini API WITH template context
     *
     * @param userMessage         User's message
     * @param conversationHistory Previous messages
     * @param templateContext     Template context string
     * @return JsonObject request body
     */
    private JsonObject buildRequestBodyWithContext(
            String userMessage,
            List<ChatMessage> conversationHistory,
            String templateContext) {

        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();

        // Add system prompt + template context as first message
        JsonObject systemContent = new JsonObject();
        systemContent.addProperty("role", "user");
        JsonArray systemParts = new JsonArray();
        JsonObject systemPart = new JsonObject();
        systemPart.addProperty("text", SYSTEM_PROMPT + templateContext);
        systemParts.add(systemPart);
        systemContent.add("parts", systemParts);
        contents.add(systemContent);

        // Add conversation history
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            // Take last 10 messages to avoid context overflow
            int startIndex = Math.max(0, conversationHistory.size() - 10);
            for (int i = startIndex; i < conversationHistory.size(); i++) {
                ChatMessage msg = conversationHistory.get(i);
                JsonObject msgContent = new JsonObject();

                // Map role: USER -> user, ASSISTANT -> model
                String role = msg.getRole() == ChatMessage.MessageRole.USER ? "user" : "model";
                msgContent.addProperty("role", role);

                JsonArray msgParts = new JsonArray();
                JsonObject msgPart = new JsonObject();
                msgPart.addProperty("text", msg.getContent());
                msgParts.add(msgPart);
                msgContent.add("parts", msgParts);

                contents.add(msgContent);
            }
        }

        // Add current user message
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        JsonArray userParts = new JsonArray();
        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", sanitizeInput(userMessage));
        userParts.add(userPart);
        userContent.add("parts", userParts);
        contents.add(userContent);

        requestBody.add("contents", contents);

        // Add generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.7);
        generationConfig.addProperty("topK", 40);
        generationConfig.addProperty("topP", 0.95);
        generationConfig.addProperty("maxOutputTokens", 1024);
        requestBody.add("generationConfig", generationConfig);

        return requestBody;
    }

    /**
     * Calls Gemini API and returns response text
     *
     * @param requestBody JSON request body
     * @return Response text
     * @throws IOException if API call fails
     */
    private String callGeminiAPI(JsonObject requestBody) throws IOException {
        // Validate API key
        if (geminiConfig.getApiKey() == null || geminiConfig.getApiKey().isBlank()) {
            log.error("GEMINI_API_KEY is not configured!");
            throw new GeminiApiException("GEMINI_API_KEY is not configured in local runtime configuration");
        }

        String apiUrl = String.format("%s/v1beta/models/%s:generateContent?key=%s",
                geminiConfig.getBaseUrl(),
                geminiConfig.getModel(),
                geminiConfig.getApiKey());

        log.info("Calling Gemini API: {} with model {}", geminiConfig.getBaseUrl(), geminiConfig.getModel());
        log.debug("Request body: {}", requestBody.toString());

        RequestBody body = RequestBody.create(requestBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(apiUrl)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("Gemini API error: HTTP {} - {}", response.code(), errorBody);
                throw new GeminiApiException("Gemini API returned error: " + response.code() + " - " + errorBody);
            }

            String responseBody = response.body().string();
            log.info("Gemini API call successful, response length: {} chars", responseBody.length());
            log.debug("Full Gemini API response: {}", responseBody);

            // Parse response
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            return extractTextFromResponse(jsonResponse);
        }
    }

    /**
     * Extracts text response from Gemini API JSON response
     *
     * @param jsonResponse JSON response from API
     * @return Extracted text
     */
    private String extractTextFromResponse(JsonObject jsonResponse) {
        try {
            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                JsonObject content = firstCandidate.getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");
                if (parts != null && parts.size() > 0) {
                    JsonObject firstPart = parts.get(0).getAsJsonObject();
                    return firstPart.get("text").getAsString();
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Gemini response", e);
        }

        return "Xin lỗi, tôi không thể xử lý yêu cầu của bạn lúc này. Vui lòng thử lại.";
    }
}
