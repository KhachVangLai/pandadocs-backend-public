package com.pandadocs.api.controller;

import com.pandadocs.api.dto.MessageResponse;
import com.pandadocs.api.dto.SuggestionRequest;
import com.pandadocs.api.model.Suggestion;
import com.pandadocs.api.model.User;
import com.pandadocs.api.repository.SuggestionRepository;
import com.pandadocs.api.repository.UserRepository;
import com.pandadocs.api.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestController
@RequestMapping("/api/suggestions")
public class SuggestionController {

    @Autowired private SuggestionRepository suggestionRepository;
    @Autowired private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createSuggestion(@RequestBody SuggestionRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userRepository.findById(userDetails.getId()).get();

        Suggestion suggestion = new Suggestion();
        suggestion.setUser(currentUser);
        suggestion.setMessage(request.getMessage());
        suggestion.setCreatedAt(Instant.now());

        suggestionRepository.save(suggestion);

        return ResponseEntity.ok(new MessageResponse("Suggestion submitted successfully!"));
    }
}