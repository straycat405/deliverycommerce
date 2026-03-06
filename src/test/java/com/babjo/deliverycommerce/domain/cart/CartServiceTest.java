package com.babjo.deliverycommerce.domain.cart;

import com.babjo.deliverycommerce.domain.cart.dto.CartItemQuantityUpdateRequestDto;
import com.babjo.deliverycommerce.domain.cart.dto.CartResponseDto;
import com.babjo.deliverycommerce.domain.cart.entity.Cart;
import com.babjo.deliverycommerce.domain.cart.entity.CartItem;
import com.babjo.deliverycommerce.domain.cart.repository.CartItemRepository;
import com.babjo.deliverycommerce.domain.cart.repository.CartRepository;
import com.babjo.deliverycommerce.domain.cart.service.CartService;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private CartService cartService;

    @Nested
    @DisplayName("내 장바구니 조회")
    class GetMyCart {

        @Test
        @DisplayName("장바구니가 없으면 빈 응답을 반환한다")
        void 장바구니가_없으면_빈_응답을_반환() {
            //given
            Long userId = 1L;

            given(cartRepository.findByUserIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

            //when
            CartResponseDto result = cartService.getMyCart(userId);

            //then
            assertThat(result).isNotNull();
            assertThat(result.getCartId()).isNull();
            assertThat(result.getStoreId()).isNull();
            assertThat(result.getItems()).isEmpty();
        }

        @Test
        @DisplayName("장바구니가 있으면 장바구니 항목 목록을 포함해 반환")
        void 장바구니가_있으면_항목_목록을_반환() {
            //given
            Long userId = 1L;
            UUID storeId = UUID.randomUUID();
            UUID productId1 = UUID.randomUUID();
            UUID productId2 = UUID.randomUUID();

            Cart cart = Cart.create(userId, storeId);

            CartItem cartItem1 = CartItem.create(cart.getCartId(), productId1, 2);
            CartItem cartItem2 = CartItem.create(cart.getCartId(), productId2, 1);

            given(cartRepository.findByUserIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(cart));

            given(cartItemRepository.findAllByCartIdAndDeletedAtIsNull(cart.getCartId())).willReturn(List.of(cartItem1, cartItem2));

            //when
            CartResponseDto result = cartService.getMyCart(userId);

            //then
            assertThat(result).isNotNull();
            assertThat(result.getCartId()).isEqualTo(cart.getCartId());
            assertThat(result.getStoreId()).isEqualTo(storeId);
            assertThat(result.getItems()).hasSize(2);

            assertThat(result.getItems().get(0).getProductId()).isEqualTo(productId1);
            assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);

            assertThat(result.getItems().get(1).getProductId()).isEqualTo(productId2);
            assertThat(result.getItems().get(1).getQuantity()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("장바구니 수량 수정")
    class UpdateQuantity {

        @Test
        @DisplayName("정상적으로 장바구니 항목 수량을 수정")
        void 장바구니_항목_수량_수정() throws Exception {

            //given
            Long userId = 1L;
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            Cart cart = Cart.create(userId, storeId);
            CartItem cartItem = CartItem.create(cart.getCartId(), productId, 2);

            CartItemQuantityUpdateRequestDto request = 수량수정요청(3);

            given(cartItemRepository.findByCartItemIdAndDeletedAtIsNull(cartItem.getCartItemId())).willReturn(Optional.of(cartItem));
            given(cartRepository.findByCartIdAndDeletedAtIsNull(cart.getCartId())).willReturn(Optional.of(cart));
            given(cartItemRepository.findAllByCartIdAndDeletedAtIsNull(cart.getCartId())).willReturn(List.of(cartItem));

            //when
            CartResponseDto result = cartService.updateQuantity(userId, cartItem.getCartItemId(), request);

            //then
            assertThat(cartItem.getQuantity()).isEqualTo(3);
            assertThat(result.getCartId()).isEqualTo(cart.getCartId());
            assertThat(result.getStoreId()).isEqualTo(storeId);
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getQuantity()).isEqualTo(3);

        }

        @Test
        @DisplayName("수량이 1 미만이면 INVALID_QUANTITY 예외 발생")
        void INVALID_QUANTITY_예외_발생() throws Exception {
            //given
            Long userId = 1L;
            UUID cartItemId = UUID.randomUUID();
            CartItemQuantityUpdateRequestDto request = 수량수정요청(0);

            //when & then
            assertThatThrownBy(() -> cartService.updateQuantity(userId, cartItemId, request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_QUANTITY);
        }

        @Test
        @DisplayName("장바구니 항목이 없으면 CART_ITEM_NOT_FOUND 예외 발생")
        void CART_ITEM_NOT_FOUND_예외_발생() throws Exception {
            //given
            Long userId = 1L;
            UUID cartItemId = UUID.randomUUID();
            CartItemQuantityUpdateRequestDto request = 수량수정요청(2);

            given(cartItemRepository.findByCartItemIdAndDeletedAtIsNull(cartItemId)).willReturn(Optional.empty());

            //when & then
            assertThatThrownBy(() -> cartService.updateQuantity(userId, cartItemId, request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        @Test
        @DisplayName("내 장바구니 항목이 아니면 CART_FORBIDDEN 예외 발생")
        void CART_FORBIDDEN_예외_발생() throws Exception {
            //given
            Long loginUserId = 1L;
            Long ownerUserId = 2L;
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            Cart cart = Cart.create(ownerUserId, storeId);
            CartItem cartItem = CartItem.create(cart.getCartId(), productId, 2);
            CartItemQuantityUpdateRequestDto request = 수량수정요청(3);

            given(cartItemRepository.findByCartItemIdAndDeletedAtIsNull(cartItem.getCartItemId())).willReturn(Optional.of(cartItem));
            given(cartRepository.findByCartIdAndDeletedAtIsNull(cart.getCartId())).willReturn(Optional.of(cart));

            //when & then
            assertThatThrownBy(() -> cartService.updateQuantity(loginUserId, cartItem.getCartItemId(), request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CART_FORBIDDEN);
        }

        private CartItemQuantityUpdateRequestDto 수량수정요청(Integer quantity) throws Exception {
            CartItemQuantityUpdateRequestDto request = new CartItemQuantityUpdateRequestDto();

            Field quantityField = CartItemQuantityUpdateRequestDto.class.getDeclaredField("quantity");
            quantityField.setAccessible(true);
            quantityField.set(request, quantity);

            return request;
        }
    }

    @Nested
    @DisplayName("장바구니 항목 삭제")
    class DeleteItem {

        @Test
        @DisplayName("정상적으로 장바구니 항목을 삭제(soft delete)")
        void 장바구니_항목_삭제() {
            //given
            Long userId = 1L;
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            Cart cart = Cart.create(userId, storeId);
            CartItem cartItem = CartItem.create(cart.getCartId(), productId, 2);

            given(cartItemRepository.findByCartItemIdAndDeletedAtIsNull(cartItem.getCartItemId())).willReturn(Optional.of(cartItem));

            given(cartRepository.findByCartIdAndDeletedAtIsNull(cart.getCartId())).willReturn(Optional.of(cart));

            /*마지막 항목 삭제 상황(남은 항목 없음)*/
            given(cartItemRepository.existsByCartIdAndDeletedAtIsNull(cart.getCartId())).willReturn(false);

            //when
            cartService.deleteItem(userId, cartItem.getCartItemId());

            //then
            assertThat(cartItem.isDeleted()).isTrue();
            assertThat(cartItem.getDeletedBy()).isEqualTo(userId);
            assertThat(cart.getStoreId()).isNull(); // 마지막 항목이면 초기화


        }

        @Test
        @DisplayName("삭제할 장바구니 항목이 없으면 CART_ITEM_NOT_FOUND 예외")
        void 장바구니_항목_없으면_예외() {
            //given
            Long userId = 1L;
            UUID cartItemId = UUID.randomUUID();

            given(cartItemRepository.findByCartItemIdAndDeletedAtIsNull(cartItemId)).willReturn(Optional.empty());

            //when & then
            assertThatThrownBy(() -> cartService.deleteItem(userId, cartItemId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        @Test
        @DisplayName("내 장바구니 항목이 아니면 CART_FORBIDDEN 예외")
        void 내_장바구니_항목_아니면_예외() {
            //given
            Long loginUserId = 1L;
            Long ownerUserId = 2L;
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            Cart cart = Cart.create(ownerUserId, storeId);
            CartItem cartItem = CartItem.create(cart.getCartId(), productId, 2);

            given(cartItemRepository.findByCartItemIdAndDeletedAtIsNull(cartItem.getCartItemId())).willReturn(Optional.of(cartItem));

            given(cartRepository.findByCartIdAndDeletedAtIsNull(cart.getCartId())).willReturn(Optional.of(cart));

            //when & then
            assertThatThrownBy(() -> cartService.deleteItem(loginUserId, cartItem.getCartItemId()))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CART_FORBIDDEN);
        }
    }
}
