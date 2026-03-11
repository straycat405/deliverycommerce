package com.babjo.deliverycommerce.domain.store;

import com.babjo.deliverycommerce.domain.store.dto.StoreCreateRequestDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreListResponseDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreResponseDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreUpdateRequestDto;
import com.babjo.deliverycommerce.domain.store.entity.Store;
import com.babjo.deliverycommerce.domain.store.repository.StoreRepository;
import com.babjo.deliverycommerce.domain.store.service.StoreService;
import com.babjo.deliverycommerce.domain.user.entity.User;
import com.babjo.deliverycommerce.domain.user.repository.UserRepository;
import com.babjo.deliverycommerce.global.common.enums.UserEnumRole;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    private StoreService storeService;

    @Nested
    @DisplayName("가게 생성 - 종로구 주문 가능 정책")
    class Create {

        @Test
        @DisplayName("주소가 종로구이면 생성 성공")
        void 종로구_생성() throws Exception {
            //given
            Long ownerId = 1L;

            User owner = User.createForTest(ownerId, "owner1", "owner1@test.com", "사장님", UserEnumRole.OWNER);

            given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));

            StoreCreateRequestDto request = new StoreCreateRequestDto();
            setField(request, "category", "한식");
            setField(request, "name", "한식당");
            setField(request, "address", "서울특별시 종로구 청계천로 1");

            Store saved = Store.create(ownerId, "한식", "한식당", "서울특별시 종로구 청계천로 1");
            given(storeRepository.save(any(Store.class))).willReturn(saved);

            //when
            StoreResponseDto result = storeService.create(ownerId, request);

            //then
            assertThat(result).isNotNull();
            assertThat(result.getAddress()).contains("종로구");
            then(storeRepository).should().save(any(Store.class));

        }

        @Test
        @DisplayName("주소가 종로구가 아니면 STORE_ADDRESS_NOT_SUPPORTED")
        void 종로구_아닐시_실패() throws Exception {
            //given
            Long ownerId = 1L;

            User owner = User.createForTest(ownerId, "owner1", "owner1@test.com", "사장님", UserEnumRole.OWNER);
            given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));

            StoreCreateRequestDto request = new StoreCreateRequestDto();
            setField(request, "category", "한식");
            setField(request, "name", "한식당");
            setField(request, "address", "서울특별시 강남구 테헤란로 1");

            //when & then
            assertThatThrownBy(() -> storeService.create(ownerId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException ce = (CustomException) ex;
                        assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.STORE_ADDRESS_NOT_SUPPORTED);
                    });

            then(storeRepository).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("가게 수정 종로구 주문 가능 정책")
    class Update {

        @Test
        @DisplayName("수정 후 주소가 종로구이면 성공")
        void 수정주소_종로() throws Exception {
            //given
            Long ownerId = 1L;
            UUID storeId = UUID.randomUUID();

            Store store = Store.create(ownerId, "한식", "기존가게", "서울특별시 강남구 테헤란로 1");
            setField(store, "storeId", storeId);

            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(store));

            StoreUpdateRequestDto request = new StoreUpdateRequestDto();
            setField(request, "category", "한식");
            setField(request, "name", "한식당");
            setField(request, "address", "서울특별시 종로구 새주소 1");

            //when
            StoreResponseDto result = storeService.update(storeId, ownerId, request);

            //then
            assertThat(result).isNotNull();
            assertThat(result.getAddress()).contains("종로구");
        }

        @Test
        @DisplayName("수정 후 주소가 종로구가 아니면 STORE_ADDRESS_NOT_SUPPORTED")
        void 수정후_종로구_아닐시_실패() throws Exception {
            //givne
            Long ownerId = 1L;
            UUID storeId = UUID.randomUUID();

            Store store = Store.create(ownerId, "한식", "기존가게", "서울특별시 종로구 기존주소 1");
            setField(store, "storeId", storeId);

            given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(store));

            StoreUpdateRequestDto request = new StoreUpdateRequestDto();
            setField(request, "category", "한식");
            setField(request, "name", "한식당");
            setField(request, "address", "서울특별시 강남구 테해란로 1");

            //when & then
            assertThatThrownBy(() -> storeService.update(storeId, ownerId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException ce = (CustomException) ex;
                        assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.STORE_ADDRESS_NOT_SUPPORTED);
                    });
        }
    }


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

        User owner = User.createForTest(
                ownerId,
                "owner1",
                "owner1@test.com",
                "사장님",
                UserEnumRole.OWNER
        );

        StoreCreateRequestDto request = new StoreCreateRequestDto(
                "KOREAN",
                "밥집",
                "서울시 종로구"
        );
        given(userRepository.findById(ownerId))
                .willReturn(Optional.of(owner));

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
        assertThat(response.getAddress()).isEqualTo("서울시 종로구");
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

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        try {
            var f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("setField 실패: " + target.getClass().getSimpleName() + "." + fieldName, e);
        }
       }
}