package com.babjo.deliverycommerce.domain.store;

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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
}