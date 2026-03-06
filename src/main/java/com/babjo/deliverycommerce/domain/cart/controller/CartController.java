package com.babjo.deliverycommerce.domain.cart.controller;


import com.babjo.deliverycommerce.domain.cart.dto.CartItemQuantityUpdateRequestDto;
import com.babjo.deliverycommerce.domain.cart.dto.CartResponseDto;
import com.babjo.deliverycommerce.domain.cart.service.CartService;
import com.babjo.deliverycommerce.global.security.CurrentUserResolver;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/cart")
public class CartController {

    private final CartService cartService;
    private final CurrentUserResolver currentUserResolver;

    public CartController(CartService cartService, CurrentUserResolver currentUserResolver) {
        this.cartService = cartService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public CartResponseDto getMyCart(Authentication authentication) {
        Long userId = currentUserResolver.getUserId(authentication);

        log.info("내 장바구니 조회 API 요청: userId={}", userId);

        CartResponseDto response = cartService.getMyCart(userId);

        log.info("내 장바구니 조회 API 완료: userId={}, cartId={}, itemCount={}", userId, response.getCartId(), response.getItems().size());

        return response;
    }

    @PatchMapping("/items/{cartItemId}")
    public CartResponseDto updateQuantity(@PathVariable UUID cartItemId,
                                          @Valid @RequestBody CartItemQuantityUpdateRequestDto request,
                                          Authentication authentication) {

        Long userId = currentUserResolver.getUserId(authentication);

        log.info("장바구니 수량 수정 API 요청: userId={},cartItemId={},quantity={}", userId, cartItemId, request.getQuantity());

        CartResponseDto response = cartService.updateQuantity(userId, cartItemId, request);

        log.info("장바구니 수량 수정 API 완료: userId={}, cartId={}, itemCount={}", userId, response.getCartId(), response.getItems().size());

        return response;
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID cartItemId,
                                           Authentication authentication) {

        Long userId = currentUserResolver.getUserId(authentication);

        log.info("장바구니 항목 삭제 API 요청: userId={}, cartItemId={}", userId, cartItemId);

        cartService.deleteItem(userId, cartItemId);

        log.info("장바구니 항목 삭제 API 완료: userId={}, cartItemId={}", userId, cartItemId);

        return ResponseEntity.noContent().build();
    }
}
