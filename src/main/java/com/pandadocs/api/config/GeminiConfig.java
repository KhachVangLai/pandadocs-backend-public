package com.pandadocs.api.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
@Getter
public class GeminiConfig {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String model;

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    @Value("${gemini.api.timeout}")
    private int timeout;

    // Chat rate limiting configuration
    @Value("${chat.rate-limit.messages-per-hour}")
    private int messagesPerHour;

    @Value("${chat.rate-limit.messages-per-day}")
    private int messagesPerDay;

    // Chat session configuration
    @Value("${chat.session.timeout-minutes}")
    private int sessionTimeoutMinutes;

    @Value("${chat.session.max-messages}")
    private int maxMessagesPerSession;

    @Value("${chat.auto-title.enabled}")
    private boolean autoTitleEnabled;

    @PostConstruct
    public void init() {
        if (apiKey != null) apiKey = apiKey.trim();
        if (model != null) model = model.trim();
        if (baseUrl != null) baseUrl = baseUrl.trim();

        // Log configuration (without exposing API key)
        System.out.println("Gemini Config initialized:");
        System.out.println("  Model: " + model);
        System.out.println("  Rate limit: " + messagesPerHour + "/hour, " + messagesPerDay + "/day");
        System.out.println("  Session timeout: " + sessionTimeoutMinutes + " minutes");
    }
}
