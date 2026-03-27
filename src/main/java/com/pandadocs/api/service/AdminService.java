package com.pandadocs.api.service;

import com.pandadocs.api.dto.AdminDashboardDTO;
import com.pandadocs.api.model.TemplateStatus;
import com.pandadocs.api.repository.DownloadRepository;
import com.pandadocs.api.repository.OrderItemRepository;
import com.pandadocs.api.repository.TemplateRepository;
import com.pandadocs.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AdminService {

    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private TemplateRepository templateRepository;
    @Autowired private DownloadRepository downloadRepository;
    @Autowired private UserRepository userRepository;

    public AdminDashboardDTO getDashboardStats() {
        Instant now = Instant.now();
        Instant last30Days = now.minus(30, ChronoUnit.DAYS);
        Instant last24Hours = now.minus(24, ChronoUnit.HOURS);

        double monthlyRevenue = orderItemRepository.sumRevenueSince(last30Days);
        long totalTemplates = templateRepository.count();
        long totalDownloads = downloadRepository.countByTimestampAfter(last30Days);
        long dailyUsers = userRepository.countByCreatedAtAfter(last24Hours);

        long pendingTemplates = templateRepository.countByStatus(TemplateStatus.PENDING_REVIEW);
        long approvedTemplates = templateRepository.countByStatus(TemplateStatus.APPROVED);
        long publishedTemplates = templateRepository.countByStatus(TemplateStatus.PUBLISHED);
        long rejectedTemplates = templateRepository.countByStatus(TemplateStatus.REJECTED);

        AdminDashboardDTO dto = new AdminDashboardDTO();
        dto.setMonthlyRevenue(monthlyRevenue);
        dto.setRewardCosts(0.0);
        dto.setTotalTemplates(totalTemplates);
        dto.setTotalDownloads(totalDownloads);
        dto.setDailyUsers(dailyUsers);

        dto.setPendingTemplates(pendingTemplates);
        dto.setApprovedTemplates(approvedTemplates);
        dto.setPublishedTemplates(publishedTemplates);
        dto.setRejectedTemplates(rejectedTemplates);

        return dto;
    }
}
