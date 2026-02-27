package com.babjo.deliverycommerce.domain.store.service;

import com.babjo.deliverycommerce.domain.store.dto.StoreCreateRequestDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreResponseDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreUpdateRequestDto;
import com.babjo.deliverycommerce.domain.store.entity.Store;
import com.babjo.deliverycommerce.domain.store.repository.StoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
@Service
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    /* POST /v1/stores  가게 등록*/
    public StoreResponseDto create(Long ownerId, StoreCreateRequestDto request) {
        log.info("Store create requested. ownerId={}", ownerId);

        Store store = Store.create(
                ownerId,
                request.getCategory(),
                request.getName(),
                request.getAddress()
        );

        Store saved = storeRepository.save(store);

        log.info("Store created. storeId={} ownerId={}", saved.getStoreId(), saved.getOwnerId());
        return StoreResponseDto.from(saved);
    }

    /*GET /v1/stores/{storeId} 가게 단건 조회*/
    @Transactional(readOnly = true)
    public StoreResponseDto get(UUID storeId) {
        Store store = findActiveStore(storeId);
        return StoreResponseDto.from(store);
    }

    /*PATCH /v1/stores/{storeId} 가게 수정 (주인)
     * actorUserId (지금 요청을 실행하는 사람(로그인한 유저ID)
     * ownerId와 actorUserId 비교 후 같으면 허용 아니면 거부*/
    public StoreResponseDto update(UUID storeId, Long actorUserId, StoreUpdateRequestDto request) {
        log.info("Store update requested. storeId={} actorUserId={}", storeId, actorUserId);

        Store store = findActiveStore(storeId);

        store.update(
                request.getCategory(),
                request.getName(),
                request.getAddress(),
                actorUserId
        );

        log.info("Store updated. storeId={} actorUserId={}", store.getStoreId(), actorUserId);
        return StoreResponseDto.from(store);
    }

    /*공통함수404*/
    private Store findActiveStore(UUID storeId) {
        return storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)
                .orElseThrow(() -> {
                    log.warn("Store not found or deleted. storeId={}", storeId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found");
                });
    }

    /*DELETE /v1/stores/{storeId} 가게 삭제*/
    public void delete(UUID storeId, Long actorUserId) {
        log.info("Store delete requested. storeId={} actorUserId={}", storeId, actorUserId);

        Store store = findActiveStore(storeId);
        store.softDelete(actorUserId);

        log.info("Store deleted. storeId={} actorUserId={}", store.getStoreId(), actorUserId);
    }

    private void validateOwner(Store store, Long actorUserId) {
        if (!store.getOwnerId().equals(actorUserId)) {
            log.warn("Store owner mismatch. storeId={} ownerId={} actorUserId={}", store.getStoreId(), store.getOwnerId(), actorUserId);

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not store owner");
        }
    }


}
