package com.babjo.deliverycommerce.domain.review.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;

@Getter
@NoArgsConstructor
public class ReviewCreateRequest {
    // [TODO] Order 도메인 연결 후 @NotNull 어노테이션 추가 필요
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
