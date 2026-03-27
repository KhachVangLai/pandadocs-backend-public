package com.pandadocs.api.controller;

import com.pandadocs.api.dto.LibraryItemDTO;
import com.pandadocs.api.model.Library;
import com.pandadocs.api.model.User;
import com.pandadocs.api.repository.UserRepository;
import com.pandadocs.api.repository.LibraryRepository;
import com.pandadocs.api.security.services.UserDetailsImpl;
import com.pandadocs.api.service.FirebaseStorageService;
import com.pandadocs.api.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import com.pandadocs.api.dto.MessageResponse;
import com.pandadocs.api.dto.UserDTO;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FirebaseStorageService firebaseStorageService;

     @GetMapping("/me/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserDTO> getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userRepository.findByIdWithRoles(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        UserDTO dto = new UserDTO();
        dto.setId(currentUser.getId());
        dto.setUsername(currentUser.getUsername());
        dto.setEmail(currentUser.getEmail());
        dto.setName(currentUser.getName());
        dto.setAvatar(currentUser.getAvatar());
        dto.setStatus(currentUser.getStatus() != null ? currentUser.getStatus().name() : null);
        dto.setCreatedAt(currentUser.getCreatedAt());
        dto.setRoles(currentUser.getRoles().stream().map(role -> role.getName().name()).collect(java.util.stream.Collectors.toSet()));

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/me/purchases")
    @PreAuthorize("hasRole('USER')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<LibraryItemDTO>> getCurrentUserPurchases() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();

        List<Library> libraryItems = libraryRepository.findByUserIdWithTemplateDetails(currentUserId);

        List<LibraryItemDTO> dtos = libraryItems.stream().map(item -> {
            LibraryItemDTO dto = new LibraryItemDTO();
            dto.setLibraryId(item.getId());
            dto.setAcquiredAt(item.getAcquiredAt());
            dto.setTemplate(templateService.convertToDto(item.getTemplate()));
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/me/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateUserProfile(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        if (name != null && !name.isEmpty()) {
            currentUser.setName(name);
        }

        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                // Remove the previous avatar to avoid leaving orphaned files in storage.
                if (currentUser.getAvatar() != null && !currentUser.getAvatar().isEmpty()) {
                    firebaseStorageService.deleteOldAvatar(currentUser.getId());
                }

                String avatarUrl = firebaseStorageService.uploadAvatar(avatarFile, currentUser.getId());
                currentUser.setAvatar(avatarUrl);
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error uploading avatar: " + e.getMessage()));
            }
        }

        userRepository.save(currentUser);

        return ResponseEntity.ok(new MessageResponse("User profile updated successfully!"));
    }
}
