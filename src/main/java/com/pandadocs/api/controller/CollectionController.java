package com.pandadocs.api.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pandadocs.api.dto.AddTemplateRequest;
import com.pandadocs.api.dto.CollectionDTO;
import com.pandadocs.api.dto.MessageResponse;
import com.pandadocs.api.dto.TemplateDTO;
import com.pandadocs.api.model.Collection;
import com.pandadocs.api.model.Template;
import com.pandadocs.api.model.User;
import com.pandadocs.api.repository.CollectionRepository;
import com.pandadocs.api.repository.TemplateRepository;
import com.pandadocs.api.repository.UserRepository;
import com.pandadocs.api.security.services.UserDetailsImpl;

import jakarta.persistence.EntityNotFoundException;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    @Autowired
    private CollectionRepository collectionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TemplateRepository templateRepository;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepository.findById(userDetails.getId()).get();
    }

    // Lấy tất cả collection của user
    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<CollectionDTO>> getUserCollections() {
        User currentUser = getCurrentUser();
        List<Collection> collections = collectionRepository.findByUserId(currentUser.getId());
        List<CollectionDTO> dtos = collections.stream().map(collection -> {
            CollectionDTO dto = new CollectionDTO();
            dto.setId(collection.getId());
            dto.setName(collection.getName());
            dto.setDescription(collection.getDescription());
            dto.setTemplateCount(collection.getTemplates().size());
            // Map templates
            if (collection.getTemplates() != null) {
                List<TemplateDTO> templateDtos = collection.getTemplates().stream()
                        .map(template -> {
                            TemplateDTO tdto = new TemplateDTO();
                            tdto.setId(template.getId());
                            tdto.setTitle(template.getTitle());
                            // Set other template properties as needed
                            return tdto;
                        })
                        .collect(Collectors.toList());
                dto.setTemplates(templateDtos);
            }
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // Tạo collection mới
    @PostMapping
    public ResponseEntity<CollectionDTO> createCollection(@RequestBody CollectionDTO collectionRequest) {
        User currentUser = getCurrentUser();
        Collection newCollection = new Collection();
        newCollection.setName(collectionRequest.getName());
        newCollection.setDescription(collectionRequest.getDescription());
        newCollection.setUser(currentUser);

        Collection savedCollection = collectionRepository.save(newCollection);

        // Convert entity to DTO before returning
        CollectionDTO dto = new CollectionDTO();
        dto.setId(savedCollection.getId());
        dto.setName(savedCollection.getName());
        dto.setDescription(savedCollection.getDescription());
        dto.setTemplateCount(0); // A new collection is always empty
        dto.setTemplates(new java.util.ArrayList<>());

        return ResponseEntity.ok(dto);
    }

    // Thêm template vào collection
    @PostMapping("/{collectionId}/templates")
    public ResponseEntity<?> addTemplateToCollection(@PathVariable Long collectionId,
            @RequestBody AddTemplateRequest request) {
        User currentUser = getCurrentUser();
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new EntityNotFoundException("Collection not found"));

        // Đảm bảo user chỉ có thể sửa collection của chính mình
        if (!collection.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403)
                    .body(new MessageResponse("Error: You are not authorized to modify this collection."));
        }

        Template template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));

        collection.getTemplates().add(template);
        collectionRepository.save(collection);

        return ResponseEntity.ok(new MessageResponse("Template added to collection successfully!"));
    }

    @DeleteMapping("/{collectionId}/templates/{templateId}")
    public ResponseEntity<?> removeTemplateFromCollection(@PathVariable Long collectionId,
            @PathVariable Long templateId) {
        User currentUser = getCurrentUser();
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new EntityNotFoundException("Collection not found"));

        // Đảm bảo user chỉ có thể sửa collection của chính mình
        if (!collection.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403)
                    .body(new MessageResponse("Error: You are not authorized to modify this collection."));
        }

        Template templateToRemove = templateRepository.findById(templateId)
                .orElseThrow(() -> new EntityNotFoundException("Template not found in repository"));

        // Xóa template khỏi danh sách của collection
        boolean removed = collection.getTemplates()
                .removeIf(template -> template.getId().equals(templateToRemove.getId()));

        if (removed) {
            collectionRepository.save(collection);
            return ResponseEntity.ok(new MessageResponse("Template removed from collection successfully!"));
        } else {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Template was not found in this collection."));
        }
    }

    // Cập nhật collection
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCollection(@PathVariable Long id, @RequestBody CollectionDTO collectionDetails) {
        User currentUser = getCurrentUser();
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Collection not found"));

        if (!collection.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403)
                    .body(new MessageResponse("Error: You are not authorized to update this collection."));
        }

        collection.setName(collectionDetails.getName());
        collection.setDescription(collectionDetails.getDescription());
        collectionRepository.save(collection);

        return ResponseEntity.ok(new MessageResponse("Collection updated successfully!"));
    }

    // --- API MỚI ---
    // Xóa collection
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCollection(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Collection not found"));

        if (!collection.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403)
                    .body(new MessageResponse("Error: You are not authorized to delete this collection."));
        }

        collectionRepository.delete(collection);
        return ResponseEntity.ok(new MessageResponse("Collection deleted successfully!"));
    }
}