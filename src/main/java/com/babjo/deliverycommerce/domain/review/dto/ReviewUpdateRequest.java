package com.babjo.deliverycommerce.domain.review.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Getter
@NoArgsConstructor
public class ReviewUpdateRequest {
    @Min(1)
    @Max(5)
    private Integer rating;

    private String content;
}
