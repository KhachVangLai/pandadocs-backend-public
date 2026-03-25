package com.pandadocs.api.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.Set;

@Getter
@Setter
public class OrderDTO {
    private Long id;
    private Long userId;
    private Double totalAmount;
    private Instant createdAt;
    private String status;
    private Set<OrderItemDTO> orderItems;
}