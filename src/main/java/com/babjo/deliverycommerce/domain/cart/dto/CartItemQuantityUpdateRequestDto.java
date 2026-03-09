package com.babjo.deliverycommerce.domain.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CartItemQuantityUpdateRequestDto {

    @NotNull
    @Min(1)
    private Integer quantity;
}
