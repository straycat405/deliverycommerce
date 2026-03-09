package com.babjo.deliverycommerce.domain.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CartItemAddRequestDto {

    @NotNull
    private UUID productId;

    @Min(1)
    private Integer quantity;
}
