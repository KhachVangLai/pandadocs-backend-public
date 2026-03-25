package com.pandadocs.api.exception;

/**
 * Exception thrown when there's an error calling the Gemini API
 */
public class GeminiApiException extends RuntimeException {
    public GeminiApiException(String message) {
        super(message);
    }

    public GeminiApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
