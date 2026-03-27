package com.pandadocs.api.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pandadocs.api.dto.FileUploadResponse;
import com.pandadocs.api.dto.MessageResponse;
import com.pandadocs.api.dto.ReviewDTO;
import com.pandadocs.api.dto.TemplateDTO;
import com.pandadocs.api.model.Category;
import com.pandadocs.api.model.Download;
import com.pandadocs.api.model.ERole;
import com.pandadocs.api.model.Review;
import com.pandadocs.api.model.SellerProfile;
import com.pandadocs.api.model.Template;
import com.pandadocs.api.model.TemplateStatus;
import com.pandadocs.api.model.User;
import com.pandadocs.api.repository.CategoryRepository;
import com.pandadocs.api.repository.DownloadRepository;
import com.pandadocs.api.repository.LibraryRepository;
import com.pandadocs.api.repository.ReviewRepository;
import com.pandadocs.api.repository.SellerProfileRepository;
import com.pandadocs.api.repository.TemplateRepository;
import com.pandadocs.api.repository.UserRepository;
import com.pandadocs.api.security.services.UserDetailsImpl;
import com.pandadocs.api.service.FirebaseStorageService;
import com.pandadocs.api.service.TemplateService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private static final Logger logger = LoggerFactory.getLogger(TemplateController.class);

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private DownloadRepository downloadRepository;

    @Autowired
    private SellerProfileRepository sellerProfileRepository;

    @Autowired
    private FirebaseStorageService firebaseStorageService;

    @Autowired
    private com.pandadocs.api.repository.SellerPayoutRepository sellerPayoutRepository;

    @Autowired
    private com.pandadocs.api.repository.OrderItemRepository orderItemRepository;

    @GetMapping
    public Page<TemplateDTO> getAllTemplates(
            @RequestParam(required = false, defaultValue = "") String search,
            Pageable pageable) {

        Page<Template> templatePage;

        if (search != null && !search.isEmpty()) {
            templatePage = templateRepository.findByTitleContainingIgnoreCase(search, pageable);
        } else {
            templatePage = templateRepository.findAllWithCategoryAndAuthor(pageable);
        }

        return templatePage.map(templateService::convertToDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateDTO> getTemplateById(@PathVariable Long id) {
        return templateRepository.findByIdWithCategoryAndAuthor(id)
                .map(templateService::convertToDto) 
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categories")
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<TemplateDTO> createTemplate(@RequestBody TemplateDTO templateDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User author = userRepository.findByUsernameWithRoles(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("Author not found"));

        Category category = categoryRepository.findById(templateDTO.getCategory().getId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        Template newTemplate = templateService.convertToEntity(templateDTO, author, category);

        // Admin-created templates are published immediately; seller submissions require review.
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            newTemplate.setStatus(TemplateStatus.PUBLISHED);
        } else {
            newTemplate.setStatus(TemplateStatus.PENDING_REVIEW);
        }

        Template savedTemplate = templateRepository.save(newTemplate);
        return new ResponseEntity<>(templateService.convertToDto(savedTemplate), HttpStatus.CREATED);
    }

    /**
     * Create a template by uploading its source file.
     * POST /api/templates/upload
     * Content-Type: multipart/form-data
     *
     * Form fields:
     * - file: MultipartFile (template file)
     * - title: String
     * - description: String
     * - price: BigDecimal
     * - categoryId: Long
     * - format: String[] (optional)
     * - industry: String[] (optional)
     * - isPremium: Boolean (optional)
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> uploadTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("price") String price,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "isPremium", required = false, defaultValue = "false") Boolean isPremium) {

        String fileUrl = null;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            User author = userRepository.findByIdWithRoles(userDetails.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Author not found"));

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                // Sellers must provide payout details before submitting a template.
                SellerProfile sellerProfile = sellerProfileRepository.findById(author.getId()).orElse(null);
                if (sellerProfile == null ||
                    sellerProfile.getBankName() == null || sellerProfile.getBankName().isEmpty() ||
                    sellerProfile.getBankAccountNumber() == null || sellerProfile.getBankAccountNumber().isEmpty() ||
                    sellerProfile.getBankAccountHolderName() == null || sellerProfile.getBankAccountHolderName().isEmpty()) {
                    return ResponseEntity.badRequest().body(new MessageResponse(
                        "Vui lòng cập nhật thông tin tài khoản ngân hàng trước khi upload template. " +
                        "Truy cập Seller Profile để cập nhật thông tin ngân hàng."
                    ));
                }
            }

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));

            fileUrl = firebaseStorageService.uploadTemplate(file, author.getId());

            Template newTemplate = new Template();
            newTemplate.setTitle(title);
            newTemplate.setDescription(description);
            newTemplate.setPrice(Double.parseDouble(price));
            newTemplate.setFileUrl(fileUrl);
            newTemplate.setCategory(category);
            newTemplate.setAuthor(author);
            newTemplate.setPremium(isPremium);
            newTemplate.setCreatedAt(Instant.now());
            newTemplate.setUpdatedAt(Instant.now());

            if (isAdmin) {
                newTemplate.setStatus(TemplateStatus.PUBLISHED);
            } else {
                newTemplate.setStatus(TemplateStatus.PENDING_REVIEW);
            }

            Template savedTemplate = templateRepository.save(newTemplate);
            return ResponseEntity.status(HttpStatus.CREATED).body(templateService.convertToDto(savedTemplate));

        } catch (Exception e) {
            logger.error("Error during template upload process for title '{}': {}", title, e.getMessage(), e);

            // Clean up uploaded files if persistence fails after storage succeeds.
            if (fileUrl != null) {
                logger.info("Attempting to delete orphaned Firebase file: {}", fileUrl);
                try {
                    firebaseStorageService.deleteFile(fileUrl);
                    logger.info("Successfully deleted orphaned Firebase file.");
                } catch (Exception deleteEx) {
                    logger.error("CRITICAL: Failed to delete orphaned Firebase file after an error. Manual cleanup required for: {}", fileUrl, deleteEx);
                }
            }
            
            if (e instanceof IllegalArgumentException) {
                 return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error uploading template: " + e.getMessage()));
        }
    }

    /**
     * Upload preview images for a template.
     * POST /api/templates/{id}/preview-images
     * Content-Type: multipart/form-data
     *
     * Form fields:
     * - files: MultipartFile[] (max 5 images)
     */
    @PostMapping("/{id}/preview-images")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<?> uploadPreviewImages(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {

        try {
            Template template = templateRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Template not found"));

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

            if (!template.getAuthor().getId().equals(userDetails.getId()) && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse("Error: You are not authorized to modify this template"));
            }

            List<String> imageUrls = firebaseStorageService.uploadPreviewImages(files, id);

            template.setPreviewImages(imageUrls);
            template.setUpdatedAt(Instant.now());
            templateRepository.save(template);

            FileUploadResponse response = new FileUploadResponse();
            response.setFileUrls(imageUrls);
            response.setMessage("Preview images uploaded successfully");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error uploading preview images: " + e.getMessage()));
        }
    }

    // Support partial updates by ignoring fields that are not present in the request.
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TemplateDTO> updateTemplate(@PathVariable Long id, @RequestBody TemplateDTO templateDetails) {
        Template template = templateRepository.findByIdWithCategoryAndAuthor(id)
                .orElseThrow(() -> new EntityNotFoundException("Template not found with id: " + id));

        if (templateDetails.getTitle() != null) {
            template.setTitle(templateDetails.getTitle());
        }

        if (templateDetails.getDescription() != null) {
            template.setDescription(templateDetails.getDescription());
        }

        if (templateDetails.getPrice() != null) {
            template.setPrice(templateDetails.getPrice());
        }

        if (templateDetails.getFileUrl() != null) {
            template.setFileUrl(templateDetails.getFileUrl());
        }

        if (templateDetails.getCategory() != null && templateDetails.getCategory().getId() != null) {
            Category category = categoryRepository.findById(templateDetails.getCategory().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));
            template.setCategory(category);
        }

        if (templateDetails.getStatus() != null) {
            template.setStatus(templateDetails.getStatus());
        }

        template.setUpdatedAt(Instant.now());

        templateRepository.save(template);

        // Reload the entity with eager relationships before converting it to DTO.
        Template updatedTemplate = templateRepository.findByIdWithCategoryAndAuthor(id)
                .orElseThrow(() -> new EntityNotFoundException("Template not found after update"));

        return ResponseEntity.ok(templateService.convertToDto(updatedTemplate));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id) {        logger.info("Delete template request received for template {}", id);

        return templateRepository.findById(id).map(template -> {
            // Only published templates can be deleted through this endpoint.
            if (template.getStatus() != TemplateStatus.PUBLISHED) {
                logger.warn("Attempt to delete non-PUBLISHED template (ID: {}). Status: {}", id, template.getStatus());
                return ResponseEntity.badRequest()
                    .body(new MessageResponse("Only published templates can be deleted. " +
                        "For pending or draft templates, use the status update API instead."));
            }

            // Remove related records first because not every relationship cascades.
            try {
                sellerPayoutRepository.deleteByTemplateId(id);
                logger.info("Deleted seller payouts for template ID: {}", id);

                downloadRepository.deleteByTemplateId(id);
                logger.info("Deleted downloads for template ID: {}", id);

                reviewRepository.deleteByTemplateId(id);
                logger.info("Deleted reviews for template ID: {}", id);

                libraryRepository.deleteByTemplateId(id);
                logger.info("Deleted library entries for template ID: {}", id);

            } catch (Exception e) {
                logger.error("Failed to delete related records for template ID: {}: {}", id, e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to delete related records: " + e.getMessage()));
            }

            if (template.getFileUrl() != null && !template.getFileUrl().isEmpty()) {
                try {
                    firebaseStorageService.deleteFile(template.getFileUrl());
                    logger.info("Deleted template file from Firebase: {}", template.getFileUrl());
                } catch (Exception e) {
                    // Continue deleting the database record even if storage cleanup fails.
                    logger.error("Failed to delete template file from Firebase (URL: {}): {}", template.getFileUrl(), e.getMessage(), e);
                }
            }

            if (template.getPreviewImages() != null && !template.getPreviewImages().isEmpty()) {
                for (String imageUrl : template.getPreviewImages()) {
                    try {
                        firebaseStorageService.deleteFile(imageUrl);
                        logger.info("Deleted preview image from Firebase: {}", imageUrl);
                    } catch (Exception e) {
                        // Continue deleting the database record even if storage cleanup fails.
                        logger.error("Failed to delete preview image from Firebase (URL: {}): {}", imageUrl, e.getMessage(), e);
                    }
                }
            }

            templateRepository.delete(template);
            logger.info("Deleted template record from database (ID: {})", id);

            return ResponseEntity.ok(new MessageResponse("Template and all associated data deleted successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ReviewDTO>> getReviewsForTemplate(@PathVariable Long id) {
        List<Review> reviews = reviewRepository.findByTemplateIdWithUser(id);
        List<ReviewDTO> dtos = reviews.stream().map(review -> {
            ReviewDTO dto = new ReviewDTO();
            dto.setId(review.getId());
            dto.setRating(review.getRating());
            dto.setComment(review.getComment());
            dto.setCreatedAt(review.getCreatedAt());
            dto.setUsername(review.getUser().getUsername());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{id}/reviews")
    @PreAuthorize("hasRole('USER')")
    @Transactional
    public ResponseEntity<?> addReview(@PathVariable Long id, @Valid @RequestBody ReviewDTO reviewRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userRepository.findByIdWithRoles(userDetails.getId()).orElseThrow(() -> new RuntimeException("Error: User not found"));

        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));

        Review newReview = new Review();
        newReview.setTemplate(template);
        newReview.setUser(currentUser);
        newReview.setRating(reviewRequest.getRating());
        newReview.setComment(reviewRequest.getComment());
        newReview.setCreatedAt(Instant.now());

        reviewRepository.save(newReview);

        updateTemplateRating(template);

        return new ResponseEntity<>(new MessageResponse("Review added successfully!"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> downloadTemplate(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userRepository.findByIdWithRoles(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));

        boolean hasPurchased = libraryRepository.existsByUserAndTemplate(currentUser, template);
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals(ERole.ROLE_ADMIN));

        if (!hasPurchased && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("Error: You have not purchased this template."));
        }

        // Record the download before streaming the file.
        Download downloadLog = new Download();
        downloadLog.setUser(currentUser);
        downloadLog.setTemplate(template);
        downloadLog.setTimestamp(Instant.now());
        downloadRepository.save(downloadLog);

        try {
            FirebaseStorageService.FileInfo fileInfo = firebaseStorageService.getFileInfo(template.getFileUrl());

            byte[] fileContent = firebaseStorageService.downloadFile(template.getFileUrl());

            // Encode the filename for Content-Disposition headers.
            String encodedFilename = java.net.URLEncoder.encode(fileInfo.getFilename(), "UTF-8")
                    .replaceAll("\\+", "%20");

            // Use both filename parameters for broad browser compatibility.
            String contentDisposition = String.format(
                "attachment; filename=\"%s\"; filename*=UTF-8''%s",
                fileInfo.getFilename().replaceAll("\"", "\\\\\""),
                encodedFilename
            );

            ByteArrayResource resource = new ByteArrayResource(fileContent);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
            headers.setContentType(MediaType.parseMediaType(fileInfo.getContentType()));
            headers.setContentLength(fileContent.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error downloading file for template ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error downloading file: " + e.getMessage()));
        }
    }

    @GetMapping("/popular")
    public ResponseEntity<List<TemplateDTO>> getPopularTemplates() {
        List<Template> popularTemplates = templateRepository.findTop10ByStatusOrderByDownloadsDesc(TemplateStatus.PUBLISHED);
        
        List<TemplateDTO> dtos = popularTemplates.stream()
                .map(templateService::convertToDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/templates/{id}/preview
     * Return signed preview-image URLs valid for one hour.
     */
    @GetMapping("/{id}/preview")
    @Transactional
    public ResponseEntity<List<String>> getTemplatePreviews(@PathVariable Long id) {
        Template template = templateRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Template not found"));

        // Signed URLs allow the frontend to display private Firebase images.
        List<String> signedUrls = new ArrayList<>();
        if (template.getPreviewImages() != null) {
            for (String imageUrl : template.getPreviewImages()) {
                try {
                    String signedUrl = firebaseStorageService.generateSignedUrl(imageUrl);
                    signedUrls.add(signedUrl);
                } catch (Exception e) {
                    // Fall back to the stored URL if signed URL generation fails.
                    signedUrls.add(imageUrl);
                }
            }
        }

        return ResponseEntity.ok(signedUrls);
    }

    // Recalculate the cached rating summary after a review changes.
    private void updateTemplateRating(Template template) {
        Double averageRating = reviewRepository.calculateAverageRating(template.getId());
        Long reviewCount = reviewRepository.countByTemplateId(template.getId());

        template.setRating(averageRating != null ? averageRating.floatValue() : 0f);
        template.setReviewCount(reviewCount != null ? reviewCount.intValue() : 0);

        templateRepository.save(template);
    }

}



