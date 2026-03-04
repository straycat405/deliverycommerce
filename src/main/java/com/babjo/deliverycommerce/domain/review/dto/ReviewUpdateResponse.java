package com.babjo.deliverycommerce.domain.review.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReviewUpdateResponse {
    private UUID reviewId;
    private Integer rating;
    private String content;
    private LocalDateTime updatedAt;
}
