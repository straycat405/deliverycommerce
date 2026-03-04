package com.babjo.deliverycommerce.domain.cart.entity;

import com.babjo.deliverycommerce.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "p_cart_item")
public class CartItem extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "cart_item_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID cartItemId;

    @Column(name = "cart_id", columnDefinition = "uuid", nullable = false)
    private UUID cartId;

    @Column(name = "product_id", columnDefinition = "uuid", nullable = false)
    private UUID productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    private static CartItem create(UUID cartId, UUID productId, Integer quantity) {
        CartItem cartItem = new CartItem();
        cartItem.cartItemId = UUID.randomUUID();
        cartItem.cartId = cartId;
        cartItem.productId = productId;
        cartItem.quantity = quantity;
        return cartItem;
    }

    /*같은 상품 재추가 시 수량 증가*/
    public void increaseQuantity(int amount) {
        this.quantity += amount;
    }

    public void updateQuantity(int quantity) {
        this.quantity = quantity;
    }
}
