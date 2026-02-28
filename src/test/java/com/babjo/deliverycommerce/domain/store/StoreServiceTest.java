package com.babjo.deliverycommerce.domain.store;

import com.babjo.deliverycommerce.domain.store.dto.StoreCreateRequestDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreListResponseDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreResponseDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreUpdateRequestDto;
import com.babjo.deliverycommerce.domain.store.entity.Store;
import com.babjo.deliverycommerce.domain.store.repository.StoreRepository;
import com.babjo.deliverycommerce.domain.store.service.StoreService;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private StoreService storeService;

    @Test
    @DisplayName("존재하지 않는 가게를 조회하면 STORE_NOT_FOUND 예외가 발생한다")
    void getStore_fail_whenStoreNotFound() {
        // given
        UUID storeId = UUID.randomUUID();

        given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> storeService.get(storeId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }

    @Test
    @DisplayName("가게 주인이 아닌 사용자가 수정하면 STORE_FORBIDDEN 예외가 발생한다")
    void update_fail_whenActorIsNotOwner() {

        //given
        UUID storeID = UUID.randomUUID();
        Long ownerId = 1L;
        Long actorUserId = 2L;

        Store store = Store.create(
                ownerId,
                "KOREAN",
                "밥집",
                "서울시 강남구"
        );

        StoreUpdateRequestDto request = new StoreUpdateRequestDto(
                "CHICKEN",
                "치킨집",
                "서울시 서초구"
        );

        given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeID)).willReturn(Optional.of(store));

        //when then
        assertThatThrownBy(() -> storeService.update(storeID, actorUserId, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_FORBIDDEN);
    }

    @Test
    @DisplayName("가게 주인이 아닌 사용자가 삭제하면 STORE_FORBIDDEN 예외 처리")
    void delete_fail_whenActorIsNotOwner() {
        //given
        UUID storeId = UUID.randomUUID();
        Long ownerId = 1L;
        Long actorUserId = 2L;

        Store store = Store.create(
                ownerId,
                "KOREAN",
                "밥집",
                "서울시 강남구"
        );

        given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId))
                .willReturn(Optional.of(store));

        //when & then
        assertThatThrownBy(() -> storeService.delete(storeId, actorUserId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_FORBIDDEN);
    }

    @Test
    @DisplayName("가게 생성에 성공하면 저장된 가게 정보를 반환")
    void create_success() {
        //given
        Long ownerId = 1L;

        StoreCreateRequestDto request = new StoreCreateRequestDto(
                "KOREAN",
                "밥집",
                "서울시 강남구"
        );

        Store saveStore = Store.create(
                ownerId,
                request.getCategory(),
                request.getName(),
                request.getAddress()
        );

        given(storeRepository.save(any(Store.class)))
                .willReturn(saveStore);

        //when
        StoreResponseDto response = storeService.create(ownerId, request);

        //then
        assertThat(response.getCategory()).isEqualTo("KOREAN");
        assertThat(response.getName()).isEqualTo("밥집");
        assertThat(response.getAddress()).isEqualTo("서울시 강남구");
    }

    @Test
    @DisplayName("존재하는 가게를 조회하면 가게정보를 반환")
    void getStore_success() {
        //given
        UUID storeId = UUID.randomUUID();
        Long ownerId = 1L;

        Store store = Store.create(
                ownerId,
                "KOREAN",
                "밥집",
                "서울시 강남구"
        );

        given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(store));

        //when
        StoreResponseDto response = storeService.get(storeId);

        assertThat(response.getCategory()).isEqualTo("KOREAN");
        assertThat(response.getName()).isEqualTo("밥집");
        assertThat(response.getAddress()).isEqualTo("서울시 강남구");
    }

    @Test
    @DisplayName("조건 없이 가게 목록을 조회하면 전체 가게 목록을 반환")
    void getStores_success_whenNoCondition() {
        //given
        //전체 조회시 반환할 가게 엔티티 2개
        Store store1 = Store.create(
                1L,
                "KOREAN",
                "밥집",
                "서울시 강남구"
        );

        Store store2 = Store.create(
                2L,
                "CHICKEN",
                "치킨집",
                "서울시 서초구"
        );

        List<Store> stores = List.of(store1, store2);

        given(storeRepository.findByDeletedAtIsNull(any(PageRequest.class))).willReturn(new PageImpl<>(stores));

        //when
        List<StoreListResponseDto> response = storeService.getStores(null, null, 0);

        //then
        assertThat(response).hasSize(2);
        assertThat(response.get(0).getName()).isEqualTo("밥집");
        assertThat(response.get(1).getName()).isEqualTo("치킨집");
    }

    @Test
    @DisplayName("category 조건으로 가게 목록을 조회하면 해당 카테고리 목록 반환")
    void getStores_success_whenCategoryCondition() {
        //given
        String category = "CHICKEN";

        Store store1 = Store.create(
                1L,
                "CHICKEN",
                "BHC",
                "서울시 강남구"
        );

        Store store2 = Store.create(
                2L,
                "CHICKEN",
                "교촌치킨",
                "서울시 서초구"
        );

        List<Store> stores = List.of(store1, store2);

        given(storeRepository.findByDeletedAtIsNullAndCategory(eq(category), any(Pageable.class))).willReturn(new PageImpl<>(stores));

        //when
        List<StoreListResponseDto> response = storeService.getStores(category, null, 0);

        //then
        assertThat(response).hasSize(2);
        assertThat(response.get(0).getCategory()).isEqualTo("CHICKEN");
        assertThat(response.get(1).getCategory()).isEqualTo("CHICKEN");
    }

    @Test
    @DisplayName("name 조건으로 가게 목록을 조회하면 이름 검색 결과를 반환")
    void getStores_success_whenNameCondition() {
        //given
        String name = "치킨";

        Store store1 = Store.create(
                1L,
                "CHICKEN",
                "치킨천국",
                "서울시 강남구"
        );

        Store store2 = Store.create(
                2L,
                "CHICKEN",
                "치킨치킨",
                "서울시 서초구"
        );

        List<Store> stores = List.of(store1, store2);

        given(storeRepository.findByDeletedAtIsNullAndNameContainingIgnoreCase(eq(name), any(PageRequest.class))).willReturn(new PageImpl<>(stores));

        //when
        List<StoreListResponseDto> response = storeService.getStores(null, name, 0);

        //then
        assertThat(response).hasSize(2);
        assertThat(response.get(0).getName()).contains("치킨");
        assertThat(response.get(1).getName()).contains("치킨");
    }
}