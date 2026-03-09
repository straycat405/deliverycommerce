package com.babjo.deliverycommerce.domain.cart.dto;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class CartResponseDto {

    private UUID cartId;
    private UUID storeId;
    private List<CartItemResponseDto> items;

    public CartResponseDto(UUID cartId, UUID storeId, List<CartItemResponseDto> items) {
        this.cartId = cartId;
        this.storeId = storeId;
        this.items = items;
    }

    public static CartResponseDto empty() {
        return new CartResponseDto(null, null, List.of());
    }

    public static CartResponseDto of(UUID cartId, UUID storeId, List<CartItemResponseDto> items) {
        return new CartResponseDto(cartId, storeId, items);
    }
}
