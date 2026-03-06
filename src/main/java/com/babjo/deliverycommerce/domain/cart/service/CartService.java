package com.babjo.deliverycommerce.domain.cart.service;

import com.babjo.deliverycommerce.domain.cart.dto.CartItemQuantityUpdateRequestDto;
import com.babjo.deliverycommerce.domain.cart.dto.CartItemResponseDto;
import com.babjo.deliverycommerce.domain.cart.dto.CartResponseDto;
import com.babjo.deliverycommerce.domain.cart.entity.Cart;
import com.babjo.deliverycommerce.domain.cart.entity.CartItem;
import com.babjo.deliverycommerce.domain.cart.repository.CartItemRepository;
import com.babjo.deliverycommerce.domain.cart.repository.CartRepository;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    @Transactional
    public CartResponseDto updateQuantity(Long userId, UUID cartItemId, CartItemQuantityUpdateRequestDto request) {
        Integer quantity = request.getQuantity();

        log.info("장바구니 수량 수정 요청: userId={}, cartItemId={}, quantity={}", userId, cartItemId, quantity);

        validateQuantity(quantity);

        CartItem cartItem = cartItemRepository.findByCartItemIdAndDeletedAtIsNull(cartItemId).orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));
        Cart cart = cartRepository.findByCartIdAndDeletedAtIsNull(cartItem.getCartId()).orElseThrow(() -> new CustomException(ErrorCode.CART_FORBIDDEN));

        validateCartOwner(cart, userId);

        cartItem.updateQuantity(quantity);

        log.info("장바구니 수량 수정 완료: userId={}, cartId={}, cartItemId={}, updateQuantity={}", userId, cart.getUserId(), cartItemId, quantity);

        return buildCartResponse(cart);
    }

    @Transactional
    public void deleteItem(Long userId, UUID cartItemId) {
        log.info("장바구니 항목 삭제 요청: userId={}, cartItemId={}", userId, cartItemId);

        CartItem cartItem = cartItemRepository.findByCartItemIdAndDeletedAtIsNull(cartItemId).orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

        Cart cart = cartRepository.findByCartIdAndDeletedAtIsNull(cartItem.getCartId()).orElseThrow(() -> new CustomException(ErrorCode.CART_FORBIDDEN));

        validateCartOwner(cart, userId);

        cartItem.delete(userId);

        boolean hasRemainingItems = cartItemRepository.existsByCartIdAndDeletedAtIsNull(cart.getCartId());

        if (!hasRemainingItems) {
            cart.clearStore();
            log.info("장바구니가 비어 storeId 초기화: cartId={}, userId={}", cart.getCartId(), userId);
        }

        log.info("장바구니 항목 삭제 완료: userId={}, cartId={}, cartItemId={}",
                userId, cart.getCartId(), cartItemId);
    }

    @Transactional
    public void clearCart(Long userId) {
        log.info("장바구니 비우기 요청: userId={}", userId);

        /*사용자 장바구니 조회 (없으면 그냥 종료)*/
        Cart cart = cartRepository.findByUserIdAndDeletedAtIsNull(userId).orElse(null);

        if (cart == null) {
            log.info("장바구니 비우기 대상 없음(장바구니 없음): userId={}", userId);
            return;
        }

        /*장바구니에 남아있는 활서 CartItem 조회*/
        List<CartItem> cartItems = cartItemRepository.findAllByCartIdAndDeletedAtIsNull(cart.getCartId());

        /*전부 soft delete*/
        for (int i = 0; i < cartItems.size(); i++) {
            cartItems.get(i).delete(userId);
        }

        /*장바구니가 비었으니 storeId 초기화*/
        cart.clearStore();

        log.info("장바구니 비우기 완료: userId={}, cartId={}, deletedItemCount={}", userId, cart.getCartId(), cartItems.size());
    }

    /*내 장바구니인지 확인*/
    private void validateCartOwner(Cart cart, Long userId) {
        if (!cart.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.CART_FORBIDDEN);
        }
    }

    /*수량 값 정상인지 확인*/
    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new CustomException(ErrorCode.INVALID_QUANTITY);
        }

    }

}
