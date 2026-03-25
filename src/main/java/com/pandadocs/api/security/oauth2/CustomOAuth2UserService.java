package com.pandadocs.api.security.oauth2;

import com.pandadocs.api.model.ERole;
import com.pandadocs.api.model.Role;
import com.pandadocs.api.model.User;
import com.pandadocs.api.model.UserStatus;
import com.pandadocs.api.repository.RoleRepository;
import com.pandadocs.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        logger.info("=== CustomOAuth2UserService - Loading user from Google ===");

        // Load user from Google
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Extract user info from Google
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");
        String avatar = oAuth2User.getAttribute("picture");

        logger.info("Google user info - email: {}, name: {}, googleId: {}", email, name, googleId);

        // Check if user exists in database
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            logger.info("User not found, creating new user for email: {}", email);
            // Create new user
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setAvatar(avatar);

            // Generate unique username from email
            String username = email.split("@")[0];
            // Check if username already exists, append number if needed
            String finalUsername = username;
            int counter = 1;
            while (userRepository.existsByUsername(finalUsername)) {
                finalUsername = username + counter;
                counter++;
            }
            user.setUsername(finalUsername);

            // No password for OAuth users (or generate random secure password)
            user.setPassword("OAUTH_USER_NO_PASSWORD");

            user.setGoogleId(googleId);
            user.setStatus(UserStatus.ACTIVE);
            user.setCreatedAt(Instant.now());

            // Assign default USER role
            Set<Role> roles = new HashSet<>();
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role USER is not found."));
            roles.add(userRole);
            user.setRoles(roles);

            userRepository.save(user);
            logger.info("New user created successfully with ID: {}", user.getId());
        } else {
            logger.info("Existing user found with ID: {}, updating info...", user.getId());
            // Update existing user info
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                logger.info("Linked Google account to user");
            }
            if (user.getAvatar() == null || !user.getAvatar().equals(avatar)) {
                user.setAvatar(avatar);
            }
            if (user.getName() == null || !user.getName().equals(name)) {
                user.setName(name);
            }
            userRepository.save(user);
            logger.info("User info updated successfully");
        }

        // IMPORTANT: Eager load roles to avoid LazyInitializationException
        // Force Hibernate to load roles before session closes
        user.getRoles().size();

        // Return OAuth2User with user info stored in attributes
        logger.info("Returning OAuth2UserPrincipal for user: {} with roles: {}",
            user.getUsername(),
            user.getRoles().stream().map(r -> r.getName().name()).toList());
        return new OAuth2UserPrincipal(user, oAuth2User.getAttributes());
    }
}
