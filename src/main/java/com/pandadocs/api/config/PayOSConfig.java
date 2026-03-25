package com.pandadocs.api.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@Getter
public class PayOSConfig {

    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    @Value("${payos.api-url}")
    private String apiUrl;

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.cancel-url}")
    private String cancelUrl;

    @Value("${payos.webhook-url}")
    private String webhookUrl;

    @PostConstruct
    public void init() {
        if (clientId != null) clientId = clientId.trim();
        if (apiKey != null) apiKey = apiKey.trim();
        if (checksumKey != null) checksumKey = checksumKey.trim();
    }
}
