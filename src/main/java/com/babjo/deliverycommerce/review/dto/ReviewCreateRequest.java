package com.babjo.deliverycommerce.review.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ReviewCreateRequest {
    private UUID orderId;
    private UUID storeId;
    private Integer rating;
    private String comment;
}
