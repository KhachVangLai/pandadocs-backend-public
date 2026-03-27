package com.pandadocs.api.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pandadocs.api.dto.ActivityDTO;
import com.pandadocs.api.dto.AdminDashboardDTO;
import com.pandadocs.api.dto.CreatePayoutRequest;
import com.pandadocs.api.dto.MessageResponse;
import com.pandadocs.api.dto.SellerPayoutDTO;
import com.pandadocs.api.dto.SendNotificationRequest;
import com.pandadocs.api.dto.SuggestionDTO;
import com.pandadocs.api.dto.SuggestionRequest;
import com.pandadocs.api.dto.TemplateDTO;
import com.pandadocs.api.dto.UpdateStatusRequest;
import com.pandadocs.api.dto.UserDTO;
import com.pandadocs.api.model.Notification;
import com.pandadocs.api.model.PayoutStatus;
import com.pandadocs.api.model.SellerPayout;
import com.pandadocs.api.model.SellerProfile;
import com.pandadocs.api.model.Suggestion;
import com.pandadocs.api.model.SuggestionStatus;
import com.pandadocs.api.model.Template;
import com.pandadocs.api.model.TemplateStatus;
import com.pandadocs.api.model.User;
import com.pandadocs.api.repository.NotificationRepository;
import com.pandadocs.api.repository.OrderItemRepository;
import com.pandadocs.api.repository.SellerPayoutRepository;
import com.pandadocs.api.repository.SellerProfileRepository;
import com.pandadocs.api.repository.SuggestionRepository;
import com.pandadocs.api.repository.TemplateRepository;
import com.pandadocs.api.repository.UserRepository;
import com.pandadocs.api.service.ActivityService;
import com.pandadocs.api.service.AdminService;
import com.pandadocs.api.service.TemplateService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SuggestionRepository suggestionRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private SellerPayoutRepository sellerPayoutRepository;

    @Autowired
    private SellerProfileRepository sellerProfileRepository;

    @GetMapping("/users")
    public ResponseEntity<Page<UserDTO>> getAllUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findAllWithRoles(pageable);
        Page<UserDTO> userDtoPage = userPage.map(this::convertToDto);
        return ResponseEntity.ok(userDtoPage);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        return ResponseEntity.ok(convertToDto(user));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        User updatedUser = userRepository.save(user);
        return ResponseEntity.ok(convertToDto(updatedUser));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        user.setStatus(request.getStatus());
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User status updated successfully!"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("User deleted successfully!"));
    }

    private UserDTO convertToDto(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setEmail(user.getEmail());
        userDTO.setName(user.getName());
        userDTO.setAvatar(user.getAvatar());
        if (user.getStatus() != null) {
            userDTO.setStatus(user.getStatus().name());
        }
        userDTO.setCreatedAt(user.getCreatedAt());
        userDTO.setRoles(user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet()));
        return userDTO;
    }

    @GetMapping("/users/{id}/activity")
    public ResponseEntity<List<ActivityDTO>> getUserActivity(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found with id: " + id);
        }
        return ResponseEntity.ok(activityService.getUserActivity(id));
    }

    @GetMapping("/templates")
    public ResponseEntity<Page<TemplateDTO>> getAllTemplates(
            @RequestParam(required = false) TemplateStatus status,
            Pageable pageable) {

        Page<Template> templatePage;
        if (status != null) {
            templatePage = templateRepository.findByStatus(status, pageable);
        } else {
            templatePage = templateRepository.findAllWithCategoryAndAuthor(pageable);
        }

        Page<TemplateDTO> templateDtoPage = templatePage.map(templateService::convertToDto);
        return ResponseEntity.ok(templateDtoPage);
    }

    @GetMapping("/templates/pending")
    public ResponseEntity<List<TemplateDTO>> getPendingTemplates() {
        List<Template> pending = templateRepository.findByStatus(TemplateStatus.PENDING_REVIEW);
        List<TemplateDTO> dtos = pending.stream()
                .map(templateService::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/templates/{id}/status")
    @Transactional
    public ResponseEntity<?> updateTemplateStatus(@PathVariable Long id, @RequestParam TemplateStatus status) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));

        template.setStatus(status);
        template.setUpdatedAt(Instant.now());
        templateRepository.save(template);

        // Notify the template author when the review status changes.
        User author = template.getAuthor();
        if (author != null) {
            String message = "Template '" + template.getTitle() + "' của bạn đã được cập nhật trạng thái thành "
                    + status.toString() + ".";

            Notification notification = new Notification();
            notification.setUser(author);
            notification.setMessage(message);
            notification.setCreatedAt(Instant.now());
            notificationRepository.save(notification);
        }

        // Publishing is the hook for seller payout processing.
        if (status == TemplateStatus.PUBLISHED) {
            logger.info("Admin approved template '{}'. Consider payout for seller {}", template.getTitle(), template.getAuthor().getUsername());
        }

        return ResponseEntity.ok(new MessageResponse("Template status updated to " + status));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDTO> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/suggestions")
    @Transactional(readOnly = true)
    public ResponseEntity<List<SuggestionDTO>> getAllSuggestions() {
        List<Suggestion> suggestions = suggestionRepository.findAll();

        List<SuggestionDTO> dtos = suggestions.stream().map(suggestion -> {
            SuggestionDTO dto = new SuggestionDTO();
            dto.setId(suggestion.getId());
            dto.setMessage(suggestion.getMessage());
            dto.setStatus(suggestion.getStatus());
            dto.setResponse(suggestion.getResponse());
            dto.setCreatedAt(suggestion.getCreatedAt());
            dto.setRespondedAt(suggestion.getRespondedAt());
            dto.setUsername(suggestion.getUser().getUsername());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/suggestions/{id}/respond")
    public ResponseEntity<?> respondToSuggestion(@PathVariable Long id,
            @RequestBody SuggestionRequest responseRequest) {
        Suggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Suggestion not found"));

        suggestion.setResponse(responseRequest.getMessage());
        suggestion.setStatus(SuggestionStatus.RESPONDED);
        suggestion.setRespondedAt(Instant.now());
        suggestionRepository.save(suggestion);

        return ResponseEntity.ok(new MessageResponse("Response sent successfully."));
    }

    @GetMapping("/revenue")
    public ResponseEntity<?> getRevenueStats(@RequestParam(defaultValue = "month") String period) {
        Instant now = Instant.now();
        Instant startDate;

        switch (period.toLowerCase()) {
            case "week":
                startDate = now.minus(7, ChronoUnit.DAYS);
                break;
            case "quarter":
                startDate = now.minus(90, ChronoUnit.DAYS);
                break;
            case "year":
                startDate = now.minus(365, ChronoUnit.DAYS);
                break;
            case "month":
            default:
                startDate = now.minus(30, ChronoUnit.DAYS);
                break;
        }

        double revenue = orderItemRepository.sumTotalRevenueForPeriod(startDate, now);

        return ResponseEntity.ok(java.util.Map.of("period", period, "totalRevenue", revenue));
    }

    @PostMapping("/notifications/send")
    public ResponseEntity<?> sendNotification(@RequestBody SendNotificationRequest request) {
        List<User> recipients = new ArrayList<>();

        if (request.isSendToAll()) {
            recipients = userRepository.findAll();
        } else if (request.getRecipientIds() != null && !request.getRecipientIds().isEmpty()) {
            recipients = userRepository.findAllById(request.getRecipientIds());
        } else {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Please provide recipients or set sendToAll to true."));
        }

        List<Notification> notifications = new ArrayList<>();
        for (User user : recipients) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setMessage(request.getMessage());
            notification.setCreatedAt(Instant.now());
            notifications.add(notification);
        }

        notificationRepository.saveAll(notifications);

        return ResponseEntity.ok(new MessageResponse("Notification sent to " + recipients.size() + " user(s)."));
    }

    // Seller payout management endpoints.
    /**
     * Return all seller payouts that are still pending.
     */
    @GetMapping("/payouts/pending")
    @Transactional(readOnly = true)
    public ResponseEntity<List<SellerPayoutDTO>> getPendingPayouts() {
        List<SellerPayout> payouts = sellerPayoutRepository.findAllPendingPayouts();
        List<SellerPayoutDTO> dtos = payouts.stream()
                .map(this::convertPayoutToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Return payout history for all sellers.
     */
    @GetMapping("/payouts/history")
    @Transactional(readOnly = true)
    public ResponseEntity<List<SellerPayoutDTO>> getPayoutHistory() {
        List<SellerPayout> payouts = sellerPayoutRepository.findAll();
        List<SellerPayoutDTO> dtos = payouts.stream()
                .map(this::convertPayoutToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Create a payout record for a template approval.
     */
    @PostMapping("/payouts/template/{templateId}")
    public ResponseEntity<?> createPayoutForTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody CreatePayoutRequest request) {

        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));

        if (sellerPayoutRepository.existsByTemplateId(templateId)) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Payout for this template already exists"));
        }

        User seller = template.getAuthor();
        if (seller == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Template has no author"));
        }

        SellerPayout payout = new SellerPayout();
        payout.setTemplate(template);
        payout.setSeller(seller);
        payout.setProposedPrice(template.getPrice());
        payout.setAgreedPrice(request.getAgreedPrice());
        payout.setStatus(PayoutStatus.PENDING);
        payout.setAdminNote(request.getAdminNote());
        payout.setCreatedAt(Instant.now());

        sellerPayoutRepository.save(payout);

        String message = "Admin đã phê duyệt template '" + template.getTitle()
                + "' của bạn và sẽ thanh toán " + request.getAgreedPrice() + " VND. "
                + (request.getAdminNote() != null ? "Ghi chú: " + request.getAdminNote() : "");

        Notification notification = new Notification();
        notification.setUser(seller);
        notification.setMessage(message);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);

        return ResponseEntity.ok(new MessageResponse("Payout created successfully"));
    }

    /**
     * Mark a payout as paid after the manual transfer is completed.
     */
    @PutMapping("/payouts/{payoutId}/mark-paid")
    @Transactional
    public ResponseEntity<?> markPayoutAsPaid(@PathVariable Long payoutId) {
        SellerPayout payout = sellerPayoutRepository.findById(payoutId)
                .orElseThrow(() -> new EntityNotFoundException("Payout not found"));

        if (payout.getStatus() == PayoutStatus.PAID) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Payout already marked as paid"));
        }

        payout.setStatus(PayoutStatus.PAID);
        payout.setPaidAt(Instant.now());
        sellerPayoutRepository.save(payout);

        String message = "Bạn đã nhận được thanh toán " + payout.getAgreedPrice()
                + " VND cho template '" + payout.getTemplate().getTitle() + "'.";

        Notification notification = new Notification();
        notification.setUser(payout.getSeller());
        notification.setMessage(message);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);

        return ResponseEntity.ok(new MessageResponse("Payout marked as paid"));
    }

    /**
     * Return a single payout by ID.
     */
    @GetMapping("/payouts/{payoutId}")
    @Transactional(readOnly = true)
    public ResponseEntity<SellerPayoutDTO> getPayoutById(@PathVariable Long payoutId) {
        SellerPayout payout = sellerPayoutRepository.findById(payoutId)
                .orElseThrow(() -> new EntityNotFoundException("Payout not found"));
        return ResponseEntity.ok(convertPayoutToDTO(payout));
    }

    /**
     * Convert SellerPayout entity to DTO
     */
    private SellerPayoutDTO convertPayoutToDTO(SellerPayout payout) {
        SellerPayoutDTO dto = new SellerPayoutDTO();
        dto.setId(payout.getId());
        dto.setTemplateId(payout.getTemplate().getId());
        dto.setTemplateTitle(payout.getTemplate().getTitle());
        dto.setSellerId(payout.getSeller().getId());
        dto.setSellerUsername(payout.getSeller().getUsername());

        // Include seller banking details when they are available.
        SellerProfile sellerProfile = sellerProfileRepository.findById(payout.getSeller().getId()).orElse(null);
        if (sellerProfile != null) {
            dto.setSellerBusinessName(sellerProfile.getBusinessName());
            dto.setBankName(sellerProfile.getBankName());
            dto.setBankAccountNumber(sellerProfile.getBankAccountNumber());
            dto.setBankAccountHolderName(sellerProfile.getBankAccountHolderName());
        }

        dto.setProposedPrice(payout.getProposedPrice());
        dto.setAgreedPrice(payout.getAgreedPrice());
        dto.setStatus(payout.getStatus());
        dto.setAdminNote(payout.getAdminNote());
        dto.setPaidAt(payout.getPaidAt());
        dto.setCreatedAt(payout.getCreatedAt());

        return dto;
    }
}
