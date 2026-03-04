package com.babjo.deliverycommerce.domain.cart.controller;


import com.babjo.deliverycommerce.domain.cart.dto.CartResponseDto;
import com.babjo.deliverycommerce.domain.cart.service.CartService;
import com.babjo.deliverycommerce.global.security.CurrentUserResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
