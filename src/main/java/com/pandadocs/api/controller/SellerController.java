package com.pandadocs.api.controller;

import com.pandadocs.api.dto.MessageResponse;
import com.pandadocs.api.dto.SellerDashboardDTO;
import com.pandadocs.api.dto.SellerPayoutDTO;
import com.pandadocs.api.dto.SellerProfileDTO;
import com.pandadocs.api.dto.SellerRegistrationRequest;
import com.pandadocs.api.dto.UpdateSellerProfileRequest;
import com.pandadocs.api.model.ERole;
import com.pandadocs.api.model.Role;
import com.pandadocs.api.model.SellerPayout;
import com.pandadocs.api.model.SellerProfile;
import com.pandadocs.api.model.TemplateStatus;
import com.pandadocs.api.model.User;
import com.pandadocs.api.repository.RoleRepository;
import com.pandadocs.api.repository.SellerPayoutRepository;
import com.pandadocs.api.repository.SellerProfileRepository;
import com.pandadocs.api.repository.TemplateRepository;
import com.pandadocs.api.repository.UserRepository;
import com.pandadocs.api.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/sellers")
public class SellerController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private SellerProfileRepository sellerProfileRepository;
    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private SellerPayoutRepository sellerPayoutRepository;

    @PostMapping("/register")
    @PreAuthorize("hasRole('USER')")
    @Transactional
    public ResponseEntity<?> registerAsSeller(@RequestBody SellerRegistrationRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userRepository.findById(userDetails.getId()).get();

        if (sellerProfileRepository.existsById(currentUser.getId())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User is already a seller."));
        }

        SellerProfile profile = new SellerProfile();
        profile.setUser(currentUser);
        profile.setBusinessName(request.getBusinessName());
        profile.setDescription(request.getDescription());
        sellerProfileRepository.save(profile);

        Role sellerRole = roleRepository.findByName(ERole.ROLE_SELLER)
                .orElseThrow(() -> new RuntimeException("Error: Role 'ROLE_SELLER' is not found."));
        currentUser.getRoles().add(sellerRole);
        userRepository.save(currentUser);

        return ResponseEntity.ok(new MessageResponse("User registered as a seller successfully!"));
    }

    /**
     * Return the seller profile, including payout details.
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerProfileDTO> getSellerProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userRepository.findById(userDetails.getId()).get();

        SellerProfile profile = sellerProfileRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Error: Seller profile not found."));

        SellerProfileDTO dto = new SellerProfileDTO();
        dto.setUserId(currentUser.getId());
        dto.setBusinessName(profile.getBusinessName());
        dto.setDescription(profile.getDescription());
        dto.setBankName(profile.getBankName());
        dto.setBankAccountNumber(profile.getBankAccountNumber());
        dto.setBankAccountHolderName(profile.getBankAccountHolderName());

        boolean hasBankInfo = profile.getBankName() != null &&
                              profile.getBankAccountNumber() != null &&
                              profile.getBankAccountHolderName() != null;
        dto.setHasBankInfo(hasBankInfo);

        return ResponseEntity.ok(dto);
    }

    /**
     * Update seller profile and payout details.
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> updateSellerProfile(@RequestBody UpdateSellerProfileRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userRepository.findById(userDetails.getId()).get();

        SellerProfile profile = sellerProfileRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Error: Seller profile not found."));

        profile.setBusinessName(request.getBusinessName());
        profile.setDescription(request.getDescription());

        profile.setBankName(request.getBankName());
        profile.setBankAccountNumber(request.getBankAccountNumber());
        profile.setBankAccountHolderName(request.getBankAccountHolderName());

        sellerProfileRepository.save(profile);

        return ResponseEntity.ok(new MessageResponse("Seller profile updated successfully!"));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerDashboardDTO> getSellerDashboard() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userRepository.findById(userDetails.getId()).get();

        long pendingCount = templateRepository.countByAuthorIdAndStatus(currentUser.getId(), TemplateStatus.PENDING_REVIEW);
        long approvedCount = templateRepository.countByAuthorIdAndStatus(currentUser.getId(), TemplateStatus.APPROVED);
        long publishedCount = templateRepository.countByAuthorIdAndStatus(currentUser.getId(), TemplateStatus.PUBLISHED);
        long rejectedCount = templateRepository.countByAuthorIdAndStatus(currentUser.getId(), TemplateStatus.REJECTED);

        Double totalEarnings = sellerPayoutRepository.calculateTotalEarnings(currentUser);
        Double pendingEarnings = sellerPayoutRepository.calculatePendingEarnings(currentUser);

        SellerDashboardDTO dashboardDTO = new SellerDashboardDTO();
        dashboardDTO.setPendingReviewCount(pendingCount);
        dashboardDTO.setApprovedCount(approvedCount + publishedCount);
        dashboardDTO.setRejectedCount(rejectedCount);
        dashboardDTO.setSubmittedCount(pendingCount + approvedCount + publishedCount + rejectedCount);
        dashboardDTO.setTotalEarnings(totalEarnings != null ? totalEarnings : 0.0);

        return ResponseEntity.ok(dashboardDTO);
    }

    /**
     * Return payout history for the authenticated seller.
     */
    @GetMapping("/payouts")
    @PreAuthorize("hasRole('SELLER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<SellerPayoutDTO>> getSellerPayouts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userRepository.findById(userDetails.getId()).get();

        List<SellerPayout> payouts = sellerPayoutRepository.findBySeller(currentUser);

        List<SellerPayoutDTO> dtos = payouts.stream()
                .map(this::convertPayoutToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
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
        dto.setProposedPrice(payout.getProposedPrice());
        dto.setAgreedPrice(payout.getAgreedPrice());
        dto.setStatus(payout.getStatus());
        dto.setAdminNote(payout.getAdminNote());
        dto.setPaidAt(payout.getPaidAt());
        dto.setCreatedAt(payout.getCreatedAt());
        return dto;
    }
}
