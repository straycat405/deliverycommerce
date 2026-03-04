package com.babjo.deliverycommerce.domain.cart.service;

import com.babjo.deliverycommerce.domain.cart.dto.CartItemResponseDto;
import com.babjo.deliverycommerce.domain.cart.dto.CartResponseDto;
import com.babjo.deliverycommerce.domain.cart.entity.Cart;
import com.babjo.deliverycommerce.domain.cart.entity.CartItem;
import com.babjo.deliverycommerce.domain.cart.repository.CartItemRepository;
import com.babjo.deliverycommerce.domain.cart.repository.CartRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    public CartResponseDto getMyCart(Long userId) {
        log.info("내 장바구니 조회 요청: userId={}", userId);

        return cartRepository.findByUserIdAndDeletedAtIsNull(userId)
                .map(cart -> buildCartResponse(cart))
                .orElseGet(() -> {
                    log.info("내 장바구니 없음: userId={}", userId);
                    return CartResponseDto.empty();
                });
    }

    private CartResponseDto buildCartResponse(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findAllByCartIdAndDeletedAtIsNull(cart.getCartId());

        log.info("내 장바구니 조회 성공: cartId={}, userId={}, storeId={}, itemCount={}", cart.getCartId(), cart.getUserId(), cart.getStoreId(), cartItems.size());

        List<CartItemResponseDto> itemResponses = cartItems.stream()
                .map(cartItem -> CartItemResponseDto.from(cartItem))
                .toList();

        return CartResponseDto.of(cart.getCartId(), cart.getStoreId(), itemResponses);
    }

}
