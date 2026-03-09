package com.babjo.deliverycommerce.domain.cart;

import com.babjo.deliverycommerce.domain.cart.dto.CartItemAddRequestDto;
import com.babjo.deliverycommerce.domain.cart.dto.CartItemQuantityUpdateRequestDto;
import com.babjo.deliverycommerce.domain.cart.dto.CartResponseDto;
import com.babjo.deliverycommerce.domain.cart.entity.Cart;
import com.babjo.deliverycommerce.domain.cart.entity.CartItem;
import com.babjo.deliverycommerce.domain.cart.repository.CartItemRepository;
import com.babjo.deliverycommerce.domain.cart.repository.CartRepository;
import com.babjo.deliverycommerce.domain.cart.service.CartService;
import com.babjo.deliverycommerce.domain.product.entity.Product;
import com.babjo.deliverycommerce.domain.product.repository.ProductRepository;
import com.babjo.deliverycommerce.domain.store.entity.Store;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;


@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

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

    @Nested
    @DisplayName("장바구니 비우기")
    class ClearCart {

        @Test
        @DisplayName("장바구니가 없으면 예외 없이 종료")
        void 장바구니_없으면_종료() {
            //given
            Long userId = 1L;

            given(cartRepository.findByUserIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

            //when
            cartService.clearCart(userId);

            //then

        }

        @Test
        @DisplayName("장바구니가 있으면 모든 항목을 soft delete하고 초기화")
        void 모든_항목_삭제하고_storeId_초기화() {
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
            cartService.clearCart(userId);

            //then
            assertThat(cartItem1.isDeleted()).isTrue();
            assertThat(cartItem1.getDeletedBy()).isEqualTo(userId);

            assertThat(cartItem2.isDeleted()).isTrue();
            assertThat(cartItem2.getDeletedBy()).isEqualTo(userId);

            assertThat(cart.getStoreId()).isNull();

        }

        @Test
        @DisplayName("장바구니 항목이 없어도 storeId 초기화")
        void 항목이_없어도_초기화() {
            //given
            Long userId = 1L;
            UUID storeId = UUID.randomUUID();

            Cart cart = Cart.create(userId, storeId);

            given(cartRepository.findByUserIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(cart));

            given(cartItemRepository.findAllByCartIdAndDeletedAtIsNull(cart.getCartId())).willReturn(List.of());

            //when
            cartService.clearCart(userId);

            //then
            assertThat(cart.getStoreId()).isNull();
        }
    }

    @Nested
    @DisplayName("장바구니 상품 추가")
    class AddItem {

        @Test
        @DisplayName("상품이 존재하지 않으면 PRODUCT_NOT_FOUND 예외")
        void 상품_없으면_PRODUCT_NOT_FOUND() throws Exception {
            //given
            Long userId = 1L;
            UUID productId = UUID.randomUUID();

            CartItemAddRequestDto request = 상품추가요청(productId, 2);

            given(productRepository.findById(productId)).willReturn(Optional.empty());

            //when & then
            assertThatThrownBy(() -> cartService.addItem(userId, request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        }

        @Test
        @DisplayName("장바구니가 없으면 새로 생성한 뒤 상품을 추가한다")
        void 장바구니_없으면_생성_추가() throws Exception {
            //given
            Long userId = 1L;
            UUID productId = UUID.randomUUID();
            UUID cartId = UUID.randomUUID();

            CartItemAddRequestDto request = 상품추가요청(productId, 2);

            //상품 존재
            Product product = mock(Product.class);
            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // 장바구니 없음
            given(cartRepository.findByUserIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

            // 장바구니 생성
            Cart savedCart = Cart.create(userId, null);
            given(cartRepository.save(any(Cart.class))).willReturn(savedCart);

            // 기존 cartItem 없음
            given(cartItemRepository.findByCartIdAndProductIdAndDeletedAtIsNull(savedCart.getCartId(), productId)).willReturn(Optional.empty());

            // buildCartResponse -> 다시 조회할 수 있으니 결과용
            CartItem newItem = CartItem.create(savedCart.getCartId(), productId, 2);
            given(cartItemRepository.findAllByCartIdAndDeletedAtIsNull(savedCart.getCartId())).willReturn(List.of(newItem));

            //when
            CartResponseDto result = cartService.addItem(userId, request);

            //then
            assertThat(result).isNotNull();
            assertThat(result.getCartId()).isEqualTo(savedCart.getCartId());
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getProductId()).isEqualTo(productId);
            assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);

            // 새 cartItem 저장이 호출되었는지 확인
            then(cartItemRepository).should().save(any(CartItem.class));
            // cart 생성 호출 확인
            then(cartRepository).should().save(any(Cart.class));

        }

        @Test
        @DisplayName("이미 담긴 상품이면 CartItem 수량 증가")
        void 수량증가() throws Exception {
            // given
            Long userId = 1L;
            UUID productId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();

            CartItemAddRequestDto request = 상품추가요청(productId, 3);

            // 상품 존재
            Product product = mock(Product.class);
            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // 장바구니 존재
            Cart cart = Cart.create(userId, storeId);
            given(cartRepository.findByUserIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(cart));

            // cartItem 존재(수량 = 2)
            CartItem existingItem = CartItem.create(cart.getCartId(), productId, 2);
            given(cartItemRepository.findByCartIdAndProductIdAndDeletedAtIsNull(cart.getCartId(), productId)).willReturn(Optional.of(existingItem));

            // buildCartResponse에서 조회될 리스트 증가된 기존 아이템
            given(cartItemRepository.findAllByCartIdAndDeletedAtIsNull(cart.getCartId())).willReturn(List.of(existingItem));

            // when
            CartResponseDto result = cartService.addItem(userId, request);

            // then
            assertThat(existingItem.getQuantity()).isEqualTo(5); // 2 + 3
            assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5);

            then(cartItemRepository).should(never()).save(any(CartItem.class));
        }

        @Test
        @DisplayName("quantity가 null이면 기본값으로 처리")
        void quantity가_null_1처리() throws Exception {
            // given
            Long userId = 1L;
            UUID productId = UUID.randomUUID();

            CartItemAddRequestDto request = 상품추가요청(productId, null);

            Product product = mock(Product.class);
            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            Cart cart = Cart.create(userId, null);
            given(cartRepository.findByUserIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(cart));

            given(cartItemRepository.findByCartIdAndProductIdAndDeletedAtIsNull(cart.getCartId(), productId)).willReturn(Optional.empty());

            CartItem newItem = CartItem.create(cart.getCartId(), productId, 1);
            given(cartItemRepository.findAllByCartIdAndDeletedAtIsNull(cart.getCartId())).willReturn(List.of(newItem));

            // when
            CartResponseDto result = cartService.addItem(userId, request);

            //then
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getQuantity()).isEqualTo(1);

        }

        @Test
        @DisplayName("장바구니에 다른 가게 상품을 담으면 CART_STORE_MISMATCH 예외")
        void 다른_가게_상품_추가시_CART_STORE_MISMATCH() throws Exception {
            // given
            Long userId = 1L;

            UUID cartStoreId = UUID.randomUUID();   // 장바구니가 이미 기준으로 잡고 있는 가게
            UUID productStoreId = UUID.randomUUID();    // 이번에 담으려는 가게
            UUID productId = UUID.randomUUID();

            CartItemAddRequestDto request = 상품추가요청(productId, 1);

            Cart cart = Cart.create(userId, cartStoreId);
            given(cartRepository.findByUserIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(cart));

            Product product = mock(Product.class);
            Store store = mock(Store.class);

            given(product.getStore()).willReturn(store);
            given(store.getStoreId()).willReturn(productStoreId);

            // addItem 상품 조회 로직
            given(product.isDeleted()).willReturn(false);
            given(productRepository.findById(productId)).willReturn(Optional.of(product));


            // when & then
            assertThatThrownBy(() -> cartService.addItem(userId, request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CART_STORE_MISMATCH);

        }

        private CartItemAddRequestDto 상품추가요청(UUID productId, Integer quantity) throws Exception {
            CartItemAddRequestDto request = new CartItemAddRequestDto();

            Field productIdField = CartItemAddRequestDto.class.getDeclaredField("productId");
            productIdField.setAccessible(true);
            productIdField.set(request, productId);

            Field quantityField = CartItemAddRequestDto.class.getDeclaredField("quantity");
            quantityField.setAccessible(true);
            quantityField.set(request, quantity);

            return request;
        }
    }
}
