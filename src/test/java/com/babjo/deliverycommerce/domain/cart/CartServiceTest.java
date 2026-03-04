package com.babjo.deliverycommerce.domain.cart;

import com.babjo.deliverycommerce.domain.cart.dto.CartResponseDto;
import com.babjo.deliverycommerce.domain.cart.entity.Cart;
import com.babjo.deliverycommerce.domain.cart.entity.CartItem;
import com.babjo.deliverycommerce.domain.cart.repository.CartItemRepository;
import com.babjo.deliverycommerce.domain.cart.repository.CartRepository;
import com.babjo.deliverycommerce.domain.cart.service.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
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
}
