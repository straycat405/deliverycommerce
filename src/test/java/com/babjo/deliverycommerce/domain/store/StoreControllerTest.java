package com.babjo.deliverycommerce.domain.store;

import com.babjo.deliverycommerce.domain.store.controller.StoreController;
import com.babjo.deliverycommerce.domain.store.dto.StoreCreateRequestDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreListResponseDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreResponseDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreUpdateRequestDto;
import com.babjo.deliverycommerce.domain.store.service.StoreService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StoreController.class)
public class StoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StoreService storeService;

    // StoreController가 현재 로그인 사용자 ID를 Authentication에서 꺼낼 때 사용하는 resolver
    @MockBean
    private CurrentUserResolver currentUserResolver;

    @Test
    @WithMockUser
    @DisplayName("가게 목록 조회 API 호출 시 전체 목록 반환")
    void getStores_success() throws Exception {
        // given
        StoreListResponseDto store1 = new StoreListResponseDto(
                UUID.randomUUID(),
                "한식당",
                "한식",
                4.5,
                120
        );

        StoreListResponseDto store2 = new StoreListResponseDto(
                UUID.randomUUID(),
                "치킨집",
                "치킨",
                4.8,
                300
        );

        given(storeService.getStores(null, null, 0))
                .willReturn(List.of(store1, store2));

        // when & then
        mockMvc.perform(get("/v1/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("한식당"))
                .andExpect(jsonPath("$[0].category").value("한식"))
                .andExpect(jsonPath("$[1].name").value("치킨집"))
                .andExpect(jsonPath("$[1].category").value("치킨"));
    }

    @Test
    @WithMockUser
    @DisplayName("가게 단건 조회 API 호출 시 가게 정보를 반환")
    void getStore_success() throws Exception {
        // given
        UUID storeId = UUID.randomUUID();

        StoreResponseDto response = StoreResponseDto.builder()
                .storeId(storeId)
                .ownerId(1L)
                .category("한식")
                .name("한식당")
                .address("서울시 강남구")
                .averageRating(4.5)
                .reviewCount(120)
                .build();

        given(storeService.get(storeId))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/v1/stores/{storeId}", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.ownerId").value(1L))
                .andExpect(jsonPath("$.category").value("한식"))
                .andExpect(jsonPath("$.name").value("한식당"))
                .andExpect(jsonPath("$.address").value("서울시 강남구"))
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.reviewCount").value(120));
    }

    @Test
    @WithMockUser
    @DisplayName("가게 생성 API 호출 시 생성된 가게 정보를 반환")
    void createStore_success() throws Exception {
        // given
        UUID storeId = UUID.randomUUID();

        // Authentication 기반 사용자 ID 추출 결과를 mock 처리
        given(currentUserResolver.getUserId(any(Authentication.class)))
                .willReturn(1L);

        StoreResponseDto response = StoreResponseDto.builder()
                .storeId(storeId)
                .ownerId(1L)
                .category("한식")
                .name("한식당")
                .address("서울시 강남구")
                .averageRating(0.0)
                .reviewCount(0)
                .build();

        given(storeService.create(eq(1L), any(StoreCreateRequestDto.class)))
                .willReturn(response);

        String requestBody = """
                {
                    "category": "한식",
                    "name": "한식당",
                    "address": "서울시 강남구"
                }
                """;

        // when & then
        mockMvc.perform(post("/v1/stores")
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.ownerId").value(1L))
                .andExpect(jsonPath("$.category").value("한식"))
                .andExpect(jsonPath("$.name").value("한식당"))
                .andExpect(jsonPath("$.address").value("서울시 강남구"))
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.reviewCount").value(0));
    }

    @Test
    @WithMockUser
    @DisplayName("가게 수정 API 호출 시 수정된 가게 정보를 반환")
    void updateStore_success() throws Exception {
        // given
        UUID storeId = UUID.randomUUID();

        // Authentication 기반 사용자 ID 추출 결과를 mock 처리
        given(currentUserResolver.getUserId(any(Authentication.class)))
                .willReturn(1L);

        StoreResponseDto response = StoreResponseDto.builder()
                .storeId(storeId)
                .ownerId(1L)
                .category("치킨")
                .name("치킨집")
                .address("서울시 서초구")
                .averageRating(4.3)
                .reviewCount(10)
                .build();

        given(storeService.update(eq(storeId), eq(1L), any(StoreUpdateRequestDto.class)))
                .willReturn(response);

        String requestBody = """
                {
                    "category": "치킨",
                    "name": "치킨집",
                    "address": "서울시 서초구"
                }
                """;

        // when & then
        mockMvc.perform(patch("/v1/stores/{storeId}", storeId)
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.ownerId").value(1L))
                .andExpect(jsonPath("$.category").value("치킨"))
                .andExpect(jsonPath("$.name").value("치킨집"))
                .andExpect(jsonPath("$.address").value("서울시 서초구"))
                .andExpect(jsonPath("$.averageRating").value(4.3))
                .andExpect(jsonPath("$.reviewCount").value(10));
    }

    @Test
    @WithMockUser
    @DisplayName("가게 삭제 API 호출 시 204 No Content를 반환")
    void deleteStore_success() throws Exception {
        // given
        UUID storeId = UUID.randomUUID();

        // Authentication 기반 사용자 ID 추출 결과를 mock 처리
        given(currentUserResolver.getUserId(any(Authentication.class)))
                .willReturn(1L);

        // when & then
        mockMvc.perform(delete("/v1/stores/{storeId}", storeId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}