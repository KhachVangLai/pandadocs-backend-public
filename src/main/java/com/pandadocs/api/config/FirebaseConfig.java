package com.pandadocs.api.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials.path:}")
    private String firebaseCredentialsPath;

    @Value("${firebase.credentials.base64:}")
    private String firebaseCredentialsBase64;

    @Value("${firebase.storage.bucket}")
    private String storageBucket;

    @Bean
    public FirebaseApp initializeFirebase() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(loadCredentials())
                    .setStorageBucket(storageBucket.trim())
                    .build();

            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    @Bean
    public Storage firebaseStorage() throws IOException {
        return StorageOptions.newBuilder()
                .setCredentials(loadCredentials())
                .build()
                .getService();
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (hasText(firebaseCredentialsBase64)) {
            logger.info("Loading Firebase credentials from FIREBASE_CREDENTIALS_BASE64");
            byte[] decoded = Base64.getDecoder().decode(firebaseCredentialsBase64.trim());
            try (InputStream inputStream = new ByteArrayInputStream(decoded)) {
                return GoogleCredentials.fromStream(inputStream);
            }
        }

        if (hasText(firebaseCredentialsPath)) {
            logger.info("Loading Firebase credentials from configured file path");
            try (InputStream inputStream = Files.newInputStream(Path.of(firebaseCredentialsPath.trim()))) {
                return GoogleCredentials.fromStream(inputStream);
            }
        }

        logger.info("Loading Firebase credentials from Application Default Credentials");
        return GoogleCredentials.getApplicationDefault();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
