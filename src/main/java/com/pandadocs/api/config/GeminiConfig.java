package com.pandadocs.api.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Slf4j
public class GeminiConfig {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String model;

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    @Value("${gemini.api.timeout}")
    private int timeout;

    @Value("${chat.rate-limit.messages-per-hour}")
    private int messagesPerHour;

    @Value("${chat.rate-limit.messages-per-day}")
    private int messagesPerDay;

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

        log.info(
                "Gemini chat configuration initialized with model {} and session timeout {} minutes",
                model,
                sessionTimeoutMinutes
        );
    }
}
