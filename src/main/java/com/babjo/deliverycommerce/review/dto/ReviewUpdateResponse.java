package com.babjo.deliverycommerce.review.dto;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class ReviewUpdateResponse {
    private String reviewId;
    private Integer rating;
    private String comment;
    private Timestamp updatedAt;
}
