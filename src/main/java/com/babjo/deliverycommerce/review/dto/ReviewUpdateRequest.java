package com.babjo.deliverycommerce.review.dto;

import lombok.Data;

@Data
public class ReviewUpdateRequest {
    private Integer rating;
    private String comment;
}
