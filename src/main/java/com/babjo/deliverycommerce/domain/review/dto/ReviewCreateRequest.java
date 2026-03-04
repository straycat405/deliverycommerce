package com.babjo.deliverycommerce.domain.review.dto;

import lombok.Data;

import java.util.UUID;

 import jakarta.validation.constraints.NotNull;
 import jakarta.validation.constraints.Min;
 import jakarta.validation.constraints.Max;
 import jakarta.validation.constraints.NotBlank;

@Data
public class ReviewCreateRequest {
    @NotNull
    private UUID orderId;

    @NotNull
    private UUID storeId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    private String content;
}
