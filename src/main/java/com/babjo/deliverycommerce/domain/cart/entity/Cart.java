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
@Table(name = "p_cart")
public class Cart extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "cart_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID cartId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "store_id", columnDefinition = "uuid")
    private UUID storeId;

    public static Cart create(Long userId, UUID storeId) {
        Cart cart = new Cart();
        cart.cartId = UUID.randomUUID();
        cart.userId = userId;
        cart.storeId = storeId;
        return cart;
    }

    /*첫 상품 담을 떄*/
    public void assignStore(UUID storeId) {
        this.storeId = storeId;
    }

    public void clearStore() {
        this.storeId = null;
    }
}
