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

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private TemplateRepository templateRepository; // Thêm import này

    @Autowired
    private TemplateService templateService; // Thêm import này

    @Autowired
    private AdminService adminService;

    @Autowired
    private NotificationRepository notificationRepository; // <-- Thêm dòng này

    @Autowired
    private SuggestionRepository suggestionRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private SellerPayoutRepository sellerPayoutRepository;

    @Autowired
    private SellerProfileRepository sellerProfileRepository;

    // API lấy danh sách tất cả user
    @GetMapping("/users")
    public ResponseEntity<Page<UserDTO>> getAllUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findAllWithRoles(pageable);
        Page<UserDTO> userDtoPage = userPage.map(this::convertToDto);
        return ResponseEntity.ok(userDtoPage);
    }

    // --- API MỚI ---
    // API lấy chi tiết một user
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        return ResponseEntity.ok(convertToDto(user));
    }

    // --- API MỚI ---
    // API cập nhật thông tin user (Admin có thể sửa tên, email...)
    @PutMapping("/users/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        // Thêm logic cập nhật role nếu cần

        User updatedUser = userRepository.save(user);
        return ResponseEntity.ok(convertToDto(updatedUser));
    }

    // API cập nhật trạng thái user
    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        user.setStatus(request.getStatus());
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User status updated successfully!"));
    }

    // --- API MỚI ---
    // API xóa user
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("User deleted successfully!"));
    }

    // Hàm helper để chuyển đổi User sang UserDTO
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

    // API lấy TẤT CẢ templates (cho admin quản lý)
    @GetMapping("/templates")
    public ResponseEntity<Page<TemplateDTO>> getAllTemplates(
            @RequestParam(required = false) TemplateStatus status,
            Pageable pageable) {

        Page<Template> templatePage;
        if (status != null) {
            // Nếu có filter theo status
            templatePage = templateRepository.findByStatus(status, pageable);
        } else {
            // Lấy tất cả templates với eager loading
            templatePage = templateRepository.findAllWithCategoryAndAuthor(pageable);
        }

        Page<TemplateDTO> templateDtoPage = templatePage.map(templateService::convertToDto);
        return ResponseEntity.ok(templateDtoPage);
    }

    // API lấy danh sách các template đang chờ duyệt
    @GetMapping("/templates/pending")
    public ResponseEntity<List<TemplateDTO>> getPendingTemplates() {
        List<Template> pending = templateRepository.findByStatus(TemplateStatus.PENDING_REVIEW); // Cần thêm hàm này vào
                                                                                                 // Repo
        List<TemplateDTO> dtos = pending.stream()
                .map(templateService::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // API để Admin thay đổi trạng thái của một template
    @PutMapping("/templates/{id}/status")
    @Transactional
    public ResponseEntity<?> updateTemplateStatus(@PathVariable Long id, @RequestParam TemplateStatus status) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));

        template.setStatus(status);
        template.setUpdatedAt(Instant.now()); // <-- THÊM DÒNG NÀY
        templateRepository.save(template);
        // --- THÊM LOGIC TẠO THÔNG BÁO ---
        // Gửi thông báo cho tác giả của template
        User author = template.getAuthor();
        // Chỉ gửi thông báo nếu tác giả không phải là chính admin đang duyệt
        // (Để tránh admin tự gửi thông báo cho chính mình)
        if (author != null) {
            String message = "Template '" + template.getTitle() + "' của bạn đã được cập nhật trạng thái thành "
                    + status.toString() + ".";

            Notification notification = new Notification();
            notification.setUser(author);
            notification.setMessage(message);
            notification.setCreatedAt(Instant.now());
            notificationRepository.save(notification);
        }

        // --- LOGIC MỚI: XỬ LÝ KHI TEMPLATE ĐƯỢC DUYỆT (PUBLISHED) ---
        if (status == TemplateStatus.PUBLISHED) {
            // Nếu template được duyệt, và tác giả là SELLER,
            // cần ghi nhận một khoản thanh toán (payout) cho seller.
            // Đây là nơi bạn sẽ tích hợp logic để chuyển tiền cho seller
            // hoặc ghi nhận công nợ để thanh toán định kỳ.
            // Ví dụ: payoutService.recordPayoutForSeller(template.getAuthor(), template.getPrice());
            logger.info("Admin approved template '{}'. Consider payout for seller {}", template.getTitle(), template.getAuthor().getUsername());
        }
        // -----------------------------

        return ResponseEntity.ok(new MessageResponse("Template status updated to " + status));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDTO> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // Lấy danh sách tất cả góp ý
    @GetMapping("/suggestions")
    @Transactional(readOnly = true)
    public ResponseEntity<List<SuggestionDTO>> getAllSuggestions() {
        List<Suggestion> suggestions = suggestionRepository.findAll();

        // Chuyển đổi sang DTO
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

    // Phản hồi một góp ý
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

    // --- API MỚI ---
    @PostMapping("/notifications/send")
    public ResponseEntity<?> sendNotification(@RequestBody SendNotificationRequest request) {
        List<User> recipients = new ArrayList<>();

        if (request.isSendToAll()) {
            // Nếu gửi cho tất cả, lấy toàn bộ user
            recipients = userRepository.findAll();
        } else if (request.getRecipientIds() != null && !request.getRecipientIds().isEmpty()) {
            // Nếu có danh sách ID, chỉ lấy các user đó
            recipients = userRepository.findAllById(request.getRecipientIds());
        } else {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Please provide recipients or set sendToAll to true."));
        }

        // Tạo thông báo cho từng người nhận
        List<Notification> notifications = new ArrayList<>();
        for (User user : recipients) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setMessage(request.getMessage());
            notification.setCreatedAt(Instant.now());
            notifications.add(notification);
        }

        // Lưu tất cả thông báo vào database
        notificationRepository.saveAll(notifications);

        return ResponseEntity.ok(new MessageResponse("Notification sent to " + recipients.size() + " user(s)."));
    }

    // ========================================================================
    // SELLER PAYOUT MANAGEMENT ENDPOINTS
    // ========================================================================
    /**
     * Lấy danh sách tất cả seller payouts đang pending
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
     * Lấy lịch sử tất cả payouts (đã trả và chưa trả)
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
     * Tạo payout cho seller khi approve template
     * Admin nhập số tiền đồng ý trả
     */
    @PostMapping("/payouts/template/{templateId}")
    public ResponseEntity<?> createPayoutForTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody CreatePayoutRequest request) {

        // 1. Tìm template
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));

        // 2. Kiểm tra template đã có payout chưa
        if (sellerPayoutRepository.existsByTemplateId(templateId)) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Payout for this template already exists"));
        }

        // 3. Kiểm tra author có phải seller không
        User seller = template.getAuthor();
        if (seller == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Template has no author"));
        }

        // 4. Tạo SellerPayout record
        SellerPayout payout = new SellerPayout();
        payout.setTemplate(template);
        payout.setSeller(seller);
        payout.setProposedPrice(template.getPrice()); // Giá seller đề xuất
        payout.setAgreedPrice(request.getAgreedPrice()); // Giá admin đồng ý trả
        payout.setStatus(PayoutStatus.PENDING);
        payout.setAdminNote(request.getAdminNote());
        payout.setCreatedAt(Instant.now());

        sellerPayoutRepository.save(payout);

        // 5. Gửi notification cho seller
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
     * Đánh dấu payout đã chuyển tiền (admin confirm đã chuyển khoản thủ công)
     */
    @PutMapping("/payouts/{payoutId}/mark-paid")
    @Transactional
    public ResponseEntity<?> markPayoutAsPaid(@PathVariable Long payoutId) {
        // 1. Tìm payout
        SellerPayout payout = sellerPayoutRepository.findById(payoutId)
                .orElseThrow(() -> new EntityNotFoundException("Payout not found"));

        // 2. Kiểm tra status
        if (payout.getStatus() == PayoutStatus.PAID) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Payout already marked as paid"));
        }

        // 3. Cập nhật status
        payout.setStatus(PayoutStatus.PAID);
        payout.setPaidAt(Instant.now());
        sellerPayoutRepository.save(payout);

        // 4. Gửi notification cho seller
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
     * Lấy thông tin chi tiết một payout
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

        // Lấy thông tin SellerProfile nếu có
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