package com.babjo.deliverycommerce.domain.cart.service;

import com.babjo.deliverycommerce.domain.cart.dto.CartItemAddRequestDto;
import com.babjo.deliverycommerce.domain.cart.dto.CartItemQuantityUpdateRequestDto;
import com.babjo.deliverycommerce.domain.cart.dto.CartItemResponseDto;
import com.babjo.deliverycommerce.domain.cart.dto.CartResponseDto;
import com.babjo.deliverycommerce.domain.cart.entity.Cart;
import com.babjo.deliverycommerce.domain.cart.entity.CartItem;
import com.babjo.deliverycommerce.domain.cart.repository.CartItemRepository;
import com.babjo.deliverycommerce.domain.cart.repository.CartRepository;
import com.babjo.deliverycommerce.domain.product.entity.Product;
import com.babjo.deliverycommerce.domain.product.repository.ProductRepository;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
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

        log.info("장바구니 수량 수정 완료: userId={}, cartId={}, cartItemId={}, updateQuantity={}", userId, cart.getCartId(), cartItemId, quantity);

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

    @Transactional
    public CartResponseDto addItem(Long userId, CartItemAddRequestDto request) {
        UUID productId = request.getProductId();
        Integer quantity = Optional.ofNullable(request.getQuantity()).orElse(1);

        log.info("장바구니 상품 추가 요청: userId={}, productId={}, quantity={}", userId, productId, quantity);
        validateQuantity(quantity);

        /*상품 존재/ 삭제 여부 확인 (deletedAt is null)*/
        Product product = productRepository.findById(productId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        /*내 장바구니 조회(없으면  생성)*/
        Cart cart = cartRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.create(userId, null);
                    Cart saved = cartRepository.save(newCart);
                    log.info("장바구니 신규 생성: userId={}, cartId={}", userId, saved.getCartId());
                    return saved;
                });

        /*같은 상품이 이미 담겨 있으면 수량 증가, 없으면 생성*/
        CartItem cartItem = cartItemRepository.findByCartIdAndProductIdAndDeletedAtIsNull(cart.getCartId(), productId).orElse(null);

        if (cartItem != null) {
            cartItem.increaseQuantity(quantity);
            log.info("장바구니 상품 수량 증가: userId={}, cartId={}, productId={}, newQuantity={}", userId, cart.getCartId(), productId, cartItem.getQuantity());

        } else {
            CartItem newItem = CartItem.create(cart.getCartId(), productId, quantity);
            cartItemRepository.save(newItem);
            log.info("장바구니 상품 신규 추가: userId={}, cartId={}, productId={}, quantity={}", userId, cart.getCartId(), productId, quantity);
        }

        /*최신 장바구니 반환*/
        return buildCartResponse(cart);
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
