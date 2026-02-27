package com.babjo.deliverycommerce.review.dto;

import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
public class ReviewUpdateResponse {
    private String reviewId;
    private Integer rating;
    private String comment;
    private LocalDateTime updatedAt;
}
