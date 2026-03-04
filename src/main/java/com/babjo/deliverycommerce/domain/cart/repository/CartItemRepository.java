package com.babjo.deliverycommerce.domain.cart.repository;

import com.babjo.deliverycommerce.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findAllByCartIdAndDeletedAtIsNull(UUID cartId);

    Optional<CartItem> findByCartItemIdAndDeletedAtIsNull(UUID cartItemId);

    Optional<CartItem> findByCartIdAndProductIdAndDeletedAtIsNull(UUID cartId, UUID productId);

    /*장바구니에 남은 항목 있는지 확인용*/
    boolean existsCartIdAndDeletedAtIsNull(UUID cartId);
}
