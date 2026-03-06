package com.babjo.deliverycommerce.domain.review.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewUpdateResponse {
    private UUID reviewId;
    private Integer rating;
    private String content;
    private LocalDateTime updatedAt;
}
