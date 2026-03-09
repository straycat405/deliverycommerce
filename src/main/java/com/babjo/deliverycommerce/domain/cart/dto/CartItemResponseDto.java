package com.babjo.deliverycommerce.domain.cart.dto;

import com.babjo.deliverycommerce.domain.cart.entity.CartItem;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CartItemResponseDto {

    private UUID cartItemId;
    private UUID productId;
    private Integer quantity;

    public CartItemResponseDto(UUID cartItemId, UUID productId, Integer quantity) {
        this.cartItemId = cartItemId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public static CartItemResponseDto from(CartItem cartItem) {
        return new CartItemResponseDto(
                cartItem.getCartItemId(),
                cartItem.getProductId(),
                cartItem.getQuantity()
        );
    }
}
