package com.babjo.deliverycommerce.domain.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ProductUpdateRequestDto {

    @NotBlank
    private String name;

    @NotBlank
    private Integer price;

    @NotBlank
    private String productCategory;

    private String description;

    @NotBlank
    private Boolean useAiDescription;
}
