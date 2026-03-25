package com.pandadocs.api.security.oauth2;

import com.pandadocs.api.security.jwt.JwtUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${app.oauth2.frontend-redirect-url}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        logger.info("=== OAuth2 Login Success Handler called ===");

        // Get OAuth2 user principal
        OAuth2UserPrincipal oAuth2UserPrincipal = (OAuth2UserPrincipal) authentication.getPrincipal();

        logger.info("User authenticated: {}, email: {}",
            oAuth2UserPrincipal.getUsername(),
            oAuth2UserPrincipal.getEmail());

        // Generate JWT token
        String jwt = jwtUtils.generateTokenFromUsername(oAuth2UserPrincipal.getUsername());
        logger.info("JWT token generated for user: {}", oAuth2UserPrincipal.getUsername());

        // Redirect to frontend with JWT token in URL
        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("token", jwt)
                .queryParam("username", oAuth2UserPrincipal.getUsername())
                .queryParam("email", oAuth2UserPrincipal.getEmail())
                .build()
                .toUriString();

        logger.info("Redirecting to frontend: {}", targetUrl);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);

        // Clean up security context after successful authentication
        clearAuthenticationAttributes(request);
    }
}
