package com.babjo.deliverycommerce.domain.cart.repository;

import com.babjo.deliverycommerce.domain.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<Cart> findByCartIdAndDeletedAtIsNull(UUID cartId);
}
