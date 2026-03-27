package com.pandadocs.api.security.oauth2;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pandadocs.api.model.ERole;
import com.pandadocs.api.model.Role;
import com.pandadocs.api.model.User;
import com.pandadocs.api.model.UserStatus;
import com.pandadocs.api.repository.RoleRepository;
import com.pandadocs.api.repository.UserRepository;

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
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");
        String avatar = oAuth2User.getAttribute("picture");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            logger.info("Creating new local account from Google OAuth2 login");
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setAvatar(avatar);

            String username = email.split("@")[0];
            String finalUsername = username;
            int counter = 1;
            while (userRepository.existsByUsername(finalUsername)) {
                finalUsername = username + counter;
                counter++;
            }
            user.setUsername(finalUsername);
            user.setPassword("OAUTH_USER_NO_PASSWORD");
            user.setGoogleId(googleId);
            user.setStatus(UserStatus.ACTIVE);
            user.setCreatedAt(Instant.now());

            Set<Role> roles = new HashSet<>();
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role USER is not found."));
            roles.add(userRole);
            user.setRoles(roles);

            userRepository.save(user);
            logger.info("Created OAuth2-backed user account {}", user.getId());
        } else {
            logger.info("Updating existing user from Google OAuth2 login");
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
            }
            if (user.getAvatar() == null || !user.getAvatar().equals(avatar)) {
                user.setAvatar(avatar);
            }
            if (user.getName() == null || !user.getName().equals(name)) {
                user.setName(name);
            }
            userRepository.save(user);
        }

        user.getRoles().size();
        logger.info("Prepared OAuth2 principal for user {}", user.getId());
        return new OAuth2UserPrincipal(user, oAuth2User.getAttributes());
    }
}
