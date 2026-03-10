package com.babjo.deliverycommerce.domain.cart;


import com.babjo.deliverycommerce.domain.cart.controller.CartController;
import com.babjo.deliverycommerce.domain.cart.dto.CartItemAddRequestDto;
import com.babjo.deliverycommerce.domain.cart.dto.CartItemQuantityUpdateRequestDto;
import com.babjo.deliverycommerce.domain.cart.dto.CartItemResponseDto;
import com.babjo.deliverycommerce.domain.cart.dto.CartResponseDto;
import com.babjo.deliverycommerce.domain.cart.service.CartService;
import com.babjo.deliverycommerce.global.security.CurrentUserResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


@WebMvcTest(CartController.class)
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @MockBean
    private CurrentUserResolver currentUserResolver;

    @Test
    @WithMockUser
    @DisplayName("내 장바구니 조회 API 호출 시 장바구니 정보 반환")
    void 장바구니_조회_API_호출시_장바구니_정보_반환() throws Exception {
        //given
        UUID cartId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID cartItemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        CartItemResponseDto item = new CartItemResponseDto(cartItemId, productId, 2);
        CartResponseDto response = new CartResponseDto(cartId, storeId, List.of(item));

        given(currentUserResolver.getUserId(any(Authentication.class))).willReturn(1L);
        given(cartService.getMyCart(1L)).willReturn(response);

        //when & then
        mockMvc.perform(get("/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cartId").value(cartId.toString()))
                .andExpect(jsonPath("$.data.storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].cartItemId").value(cartItemId.toString()))
                .andExpect(jsonPath("$.data.items[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));
    }

    @Test
    @WithMockUser
    @DisplayName("장바구니가 없으면 빈 장바구니 응답 반환")
    void 빈_장바구니_응답_반환() throws Exception {
        //given
        CartResponseDto emptyResponse = CartResponseDto.empty();

        given(currentUserResolver.getUserId(any(Authentication.class))).willReturn(1L);
        given(cartService.getMyCart(1L)).willReturn(emptyResponse);

        //when & then
        mockMvc.perform(get("/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cartId").doesNotExist())
                .andExpect(jsonPath("$.data.storeId").doesNotExist())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    @WithMockUser
    @DisplayName("장바구니 수량 수정 API 호출 시 수정된 장바구니 정보 반환")
    void 수정된_장바구니_정보_반환() throws Exception {
        //given
        UUID cartItemId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        String requestBody = """
                {
                    "quantity":3
                }
                """;

        CartItemResponseDto item = new CartItemResponseDto(UUID.randomUUID(), productId, 3);
        CartResponseDto response = new CartResponseDto(cartId, storeId, List.of(item));

        given(currentUserResolver.getUserId(any(Authentication.class))).willReturn(1L);
        given(cartService.updateQuantity(eq(1L), eq(cartItemId), any(CartItemQuantityUpdateRequestDto.class))).willReturn(response);

        //when & then
        mockMvc.perform(patch("/v1/cart/items/{cartItemId}", cartItemId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cartId").value(cartId.toString()))
                .andExpect(jsonPath("$.data.storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.data.items[0].quantity").value(3));

        then(cartService).should()
                .updateQuantity(eq(1L), eq(cartItemId), any(CartItemQuantityUpdateRequestDto.class));
    }

    @Test
    @WithMockUser
    @DisplayName("수량이 1미만이면 400 Bad Request 반환")
    void BAD_REQUEST_반환() throws Exception {
        //given
        UUID pathCartItemId = UUID.randomUUID();

        String requestBody = """
                {
                  "quantity": 0
                }
                """;

        given(currentUserResolver.getUserId(any(Authentication.class))).willReturn(1L);

        //when & then
        mockMvc.perform(patch("/v1/cart/items/{cartItemId}", pathCartItemId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

    }

    @Test
    @WithMockUser
    @DisplayName("장바구니 항목 삭제 API 호출 시 NO CONTENT 반환")
    void 삭제_API_호출시_204_반환() throws Exception {
        //given
        UUID cartItemId = UUID.randomUUID();

        given(currentUserResolver.getUserId(any(Authentication.class))).willReturn(1L);

        //when & then
        mockMvc.perform(delete("/v1/cart/items/{cartItemId}", cartItemId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        //서비스 호출 검증
        then(cartService).should().deleteItem(eq(1L), eq(cartItemId));
    }

    @Test
    @WithMockUser
    @DisplayName("장바구니 비우기 API 호출 시 NO CONTENT 반환")
    void 비우기_API_호출시_204_반환() throws Exception {
        //given
        given(currentUserResolver.getUserId(any(Authentication.class))).willReturn(1L);

        //when & then
        mockMvc.perform(delete("/v1/cart")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        //then
        then(cartService).should().clearCart(eq(1L));
    }

    @Test
    @WithMockUser
    @DisplayName("장바구니 상품 추가 API 호출 시 장바구니 정보를 반환")
    void 상품_추가_호출시_정보_반환() throws Exception {
        //given
        UUID productId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID cartItemId = UUID.randomUUID();

        String requestBody = """
                {
                  "productId": "%s",
                  "quantity": 2
                }
                """.formatted(productId);

        CartItemResponseDto item = new CartItemResponseDto(cartItemId, productId, 2);
        CartResponseDto response = new CartResponseDto(cartId, storeId, List.of(item));

        // 컨트롤러는 resolver로 userId를 뽑으니, 테스트에선 resolver가 1L을 반환하도록 고정
        given(currentUserResolver.getUserId(any(Authentication.class)))
                .willReturn(1L);

        // 컨트롤러가 서비스 호출하면, 우리가 준비한 response를 반환하게 설정
        given(cartService.addItem(eq(1L), any(CartItemAddRequestDto.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/v1/cart/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cartId").value(cartId.toString()))
                .andExpect(jsonPath("$.data.storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].cartItemId").value(cartItemId.toString()))
                .andExpect(jsonPath("$.data.items[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));

        // then: 컨트롤러가 서비스 메서드를 실제로 호출했는지 확인(선택이지만 강력 추천)
        then(cartService).should().addItem(eq(1L), any(CartItemAddRequestDto.class));
    }

    @Test
    @WithMockUser
    @DisplayName("productId가 없으면 400 Bad Request를 반환한다")
    void productId가_없으면_400을_반환한다() throws Exception {
        // given
        String requestBody = """
            {
              "quantity": 2
            }
            """;

        given(currentUserResolver.getUserId(any(Authentication.class)))
                .willReturn(1L);

        // when & then
        mockMvc.perform(post("/v1/cart/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("quantity가 1 미만이면 400 Bad Request를 반환한다")
    void quantity가_1_미만이면_400을_반환한다() throws Exception {
        // given
        UUID productId = UUID.randomUUID();

        String requestBody = """
            {
              "productId": "%s",
              "quantity": 0
            }
            """.formatted(productId);

        given(currentUserResolver.getUserId(any(Authentication.class)))
                .willReturn(1L);

        // when & then
        mockMvc.perform(post("/v1/cart/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
