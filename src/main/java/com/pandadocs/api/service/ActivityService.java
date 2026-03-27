package com.pandadocs.api.service;

import com.pandadocs.api.dto.ActivityDTO;
import com.pandadocs.api.repository.DownloadRepository;
import com.pandadocs.api.repository.OrderRepository;
import com.pandadocs.api.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private DownloadRepository downloadRepository;

    @Transactional(readOnly = true)
    public List<ActivityDTO> getUserActivity(Long userId) {
        List<ActivityDTO> activities = new ArrayList<>();

        orderRepository.findUserOrders(userId).forEach(order -> { 
            order.getOrderItems().forEach(item -> {
                activities.add(new ActivityDTO(
                    "PURCHASE",
                    "Purchased template: " + item.getTemplate().getTitle(),
                    order.getCreatedAt()
                ));
            });
        });

        reviewRepository.findUserReviews(userId).forEach(review -> { 
            activities.add(new ActivityDTO(
                "REVIEW",
                "Reviewed template: " + review.getTemplate().getTitle() + " with " + review.getRating() + " stars.",
                review.getCreatedAt()
            ));
        });

        downloadRepository.findUserDownloads(userId).forEach(download -> {
            activities.add(new ActivityDTO(
                "DOWNLOAD",
                "Downloaded template: " + download.getTemplate().getTitle(),
                download.getTimestamp()
            ));
        });

        return activities.stream()
                .sorted(Comparator.comparing(ActivityDTO::getTimestamp).reversed())
                .collect(Collectors.toList());
    }
}
