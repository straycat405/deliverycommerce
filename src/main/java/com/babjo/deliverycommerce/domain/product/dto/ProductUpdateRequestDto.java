package com.babjo.deliverycommerce.domain.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class ProductUpdateRequestDto {

    @NotBlank(message = "상품명은 필수입니다.")
    private String name;

    @NotNull(message = "가격은 필수입니다.")
    @Positive(message = "가격은 0보다 커야 합니다.")
    private Integer price;

    @NotBlank(message = "상품 카테고리는 필수입니다.")
    private String productCategory;

    private String description;

    private Boolean useAiDescription;
}
