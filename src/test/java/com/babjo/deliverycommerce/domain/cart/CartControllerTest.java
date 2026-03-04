package com.babjo.deliverycommerce.domain.cart;


import com.babjo.deliverycommerce.domain.cart.controller.CartController;
import com.babjo.deliverycommerce.domain.cart.dto.CartItemResponseDto;
import com.babjo.deliverycommerce.domain.cart.dto.CartResponseDto;
import com.babjo.deliverycommerce.domain.cart.service.CartService;
import com.babjo.deliverycommerce.global.security.CurrentUserResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                .andExpect(jsonPath("$.cartId").value(cartId.toString()))
                .andExpect(jsonPath("$.storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].cartItemId").value(cartItemId.toString()))
                .andExpect(jsonPath("$.items[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
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
                .andExpect(jsonPath("$.cartId").doesNotExist())
                .andExpect(jsonPath("$.storeId").doesNotExist())
                .andExpect(jsonPath("$.items.length()").value(0));
    }
}
