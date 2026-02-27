package com.babjo.deliverycommerce.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ProductCreateRequestDto {

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
