package com.babjo.deliverycommerce.domain.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ProductUpdateRequestDto {

    @NotBlank
    private String name;

    private Integer price;

    @NotBlank
    private String productCategory;

    private String description;

    private Boolean useAiDescription;
}
