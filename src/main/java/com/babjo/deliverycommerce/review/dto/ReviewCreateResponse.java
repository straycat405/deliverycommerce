package com.babjo.deliverycommerce.review.dto;

import java.sql.Timestamp;
import java.util.UUID;

public class ReviewCreateResponse {
    private UUID reviewId;
    private UUID orderId;
    private UUID storeId;
    private Integer rating;
    private String comment;
    private Timestamp createdAt;
}
