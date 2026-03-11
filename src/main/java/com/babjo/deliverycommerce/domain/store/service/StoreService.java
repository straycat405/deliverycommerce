package com.babjo.deliverycommerce.domain.store.service;

import com.babjo.deliverycommerce.domain.store.dto.StoreCreateRequestDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreListResponseDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreResponseDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreUpdateRequestDto;
import com.babjo.deliverycommerce.domain.store.entity.Store;
import com.babjo.deliverycommerce.domain.store.repository.StoreRepository;
import com.babjo.deliverycommerce.domain.user.entity.User;
import com.babjo.deliverycommerce.domain.user.repository.UserRepository;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class StoreService {

    // 가게 목록 조회 시 한 페이지에 보여줄 기본 개수
    private static final int PAGE_SIZE = 10;

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    public StoreService(StoreRepository storeRepository, UserRepository userRepository) {
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
    }

    /*
     * 주문 가능 구역 정책
     * 종로구 주소만 주문 가능
     */
    private static final String DELIVERY_AVAILABLE_GU = "종로구";

    /**
     * 가게 생성
     * - ownerId를 기반으로 새로운 가게를 생성
     * - 생성 완료 후 저장된 엔티티를 응답 DTO로 변환하여 반환
     */
    public StoreResponseDto create(Long ownerId, StoreCreateRequestDto request) {
        log.info("가게 생성 요청 - ownerId={}, category={}, name={}", ownerId, request.getCategory(), request.getName());

        // owner 확인
        User owner = userRepository.findById(ownerId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        //주문 가능 구역 검증(종로구만)
        validateDeliveryArea(request.getAddress());

        Store store = Store.create(
                ownerId,
                request.getCategory(),
                request.getName(),
                request.getAddress()
        );

        Store saved = storeRepository.save(store);

        log.info("가게 생성 완료. storeId={} ownerId={}", saved.getStoreId(), saved.getOwnerId());
        return StoreResponseDto.from(saved);
    }

    /**
     * 단일 가게 조회
     * - soft delete 되지 않은 가게만 조회
     * - 존재하지 않는 경우 STORE_NOT_FOUND 예외를 발생
     */
    @Transactional(readOnly = true)
    public StoreResponseDto get(UUID storeId) {
        Store store = findActiveStore(storeId);
        return StoreResponseDto.from(store);
    }

    /**
     * 가게 수정
     * - 삭제되지 않은 가게인지 확인
     * - 요청 사용자가 해당 가게 owner인지 검증
     * - owner가 아니면 STORE_FORBIDDEN 예외
     */
    public StoreResponseDto update(UUID storeId, Long actorUserId, StoreUpdateRequestDto request) {
        log.info("Store update requested. storeId={} actorUserId={}", storeId, actorUserId);

        Store store = findActiveStore(storeId);
        validateOwner(store, actorUserId);

        store.update(
                request.getCategory(),
                request.getName(),
                request.getAddress()
        );

        validateDeliveryArea(store.getAddress());

        log.info("가게 수정 완료 - storeId={}, actorUserId={}, updatedName={}",  store.getStoreId(), actorUserId, store.getName());
        return StoreResponseDto.from(store);
    }

    /**
     * 가게 삭제 (soft delete)
     * - 삭제되지 않은 가게인지 확인
     * - 요청 사용자가 해당 가게 owner인지 검증
     * - owner가 아니면 STORE_FORBIDDEN 예외를 발생
     */
    public void delete(UUID storeId, Long actorUserId) {
        log.info("가게 삭제 요청. storeId={} actorUserId={}", storeId, actorUserId);

        Store store = findActiveStore(storeId);
        validateOwner(store, actorUserId);
        store.delete(actorUserId);

        log.info("가게 삭제 완료. storeId={} actorUserId={}", store.getStoreId(), actorUserId);
    }

    /**
     * 공통 가게 조회 메서드
     * - soft delete 되지 않은 가게만 조회
     * - 조회 결과가 없으면 STORE_NOT_FOUND 예외를 발생
     *
     * 공통 메서드로 분리한 이유:
     * - get / update / delete 에서 동일한 조회 로직이 반복되기 때문
     * - soft delete 조건 누락을 방지하기 위함
     */
    private Store findActiveStore(UUID storeId) {
        return storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)
                .orElseThrow(() -> {
                    log.warn("가게 조회 실패 - 존재하지 않거나 삭제된 가게입니다. storeId={}", storeId);
                    return new CustomException(ErrorCode.STORE_NOT_FOUND);
                });
    }

    /**
     * 가게 owner 검증 메서드
     * - 현재 요청한 사용자(actorUserId)가 실제 가게 주인인지 확인
     * - 다를 경우 STORE_FORBIDDEN 예외를 발생
     *
     * 공통 메서드로 분리한 이유:
     * - update / delete 에서 동일한 권한 검증 로직이 반복되기 때문
     */
    private void validateOwner(Store store, Long actorUserId) {
        if (!store.getOwnerId().equals(actorUserId)) {
            log.warn("가게 권한 검증 실패. storeId={} ownerId={} actorUserId={}",
                    store.getStoreId(), store.getOwnerId(), actorUserId);

            throw new CustomException(ErrorCode.STORE_FORBIDDEN);
        }
    }

    /**
     * 가게 목록 조회
     * - category, name 조건 유무에 따라 조회 메서드를 분기
     * - 삭제되지 않은(deletedAt is null) 가게만 조회
     * - page는 0부터 시작
     */
    @Transactional(readOnly = true)
    public List<StoreListResponseDto> getStores(String category, String name, int page) {
        log.info("가게 목록 조회 요청 - category={}, name={}, page={}", category, name, page);

        PageRequest pageable = PageRequest.of(page, PAGE_SIZE);

        // category, name 파라미터가 실제로 들어왔는지 확인
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasName = name != null && !name.isBlank();

        List<Store> stores;

        //category + name 모두 존재시
        if (hasCategory && hasName) {
            log.info("가게 목록 조회 조건 - category + name 동시 탐색");
            stores = storeRepository
                    .findByDeletedAtIsNullAndCategoryAndNameContainingIgnoreCase(category, name, pageable)
                    .getContent();
        } else if (hasCategory) {
            // category만 존재
            log.info("가게 목록 조회 조건 - category 검색");
            stores = storeRepository
                    .findByDeletedAtIsNullAndCategory(category, pageable)
                    .getContent();
        } else if (hasName) {
            // name만 존재
            log.info("가게 목록 조회 조건 - name 검색");
            stores = storeRepository
                    .findByDeletedAtIsNullAndNameContainingIgnoreCase(name, pageable)
                    .getContent();
        } else {
            // 아무 조건 없으면 전체 조회
            log.info("가게 목록 조회 조건 - 전체조회");
            stores = storeRepository
                    .findByDeletedAtIsNull(pageable)
                    .getContent();
        }

        log.info("가게 목록 조회 완료 - page={}, resultCount={}", page, stores.size());

        //엔티티 리스트를 응답 DTO 리스트로 변환해서 반환
        return stores.stream()
                .map(store -> StoreListResponseDto.from(store))
                .toList();
    }

    private void validateDeliveryArea(String address) {
        if (address == null || address.isBlank() || !address.contains(DELIVERY_AVAILABLE_GU)) {
            throw new CustomException(ErrorCode.STORE_ADDRESS_NOT_SUPPORTED);
        }
    }
}