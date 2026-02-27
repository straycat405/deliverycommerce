package com.babjo.deliverycommerce.domain.store.controller;


import com.babjo.deliverycommerce.domain.store.dto.StoreCreateRequestDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreResponseDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreUpdateRequestDto;
import com.babjo.deliverycommerce.domain.store.service.StoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/store")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    /*POST /v1/stores 가게등록
     * (인증 붙기 전 - 임시로 헤더에서 유저ID 밭음)*/
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreResponseDto create(@RequestHeader("X-USER-ID") Long ownerId,
                                   @RequestBody StoreCreateRequestDto request) {

        log.info("[StoreController] create 요청: ownerId={}, category={}, name={}", ownerId, request.getCategory(), request.getName());

        StoreResponseDto result = storeService.create(ownerId, request);
        log.info("[StoreController] create 성공: storeId={}, ownerId={}", result.getStoreId(), ownerId);

        return result;
    }

    /*
     * GET /v1/stores/{storeId} 단건 조회*/
    @GetMapping("/{storeId}")
    public StoreResponseDto get(@PathVariable UUID storeId) {
        log.info("[StoreController] get 요청: storeId={}", storeId);

        StoreResponseDto result = storeService.get(storeId);
        log.info("[StoreController] get 성공: storeId={}", result.getStoreId());

        return result;
    }

    /*
     * PATCH /v1/stores/{storeId} 가게수정 - owner*/
    @PatchMapping("/{storeId}")
    public StoreResponseDto update(@PathVariable UUID storeId,
                                   @RequestHeader("X-USER-ID") Long actorUserId,
                                   @RequestBody StoreUpdateRequestDto request) {

        log.info("[StoreController] update 요청: storeId={}, actorUserId={}, category={}, name={}",
                storeId, actorUserId, request.getCategory(), request.getName());

        StoreResponseDto result = storeService.update(storeId, actorUserId, request);
        log.info("[StoreController] update 성공: storeId={}, actorUserId={}", result.getStoreId(), actorUserId);

        return result;
    }

    /*DELETE /v1/stores/{storeId} 가게 삭제 - owner*/
    @DeleteMapping("/{storeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID storeId,
                       @RequestHeader("X-USER-ID") Long actorUserId) {

        log.info("[StoreController] delete 요청: storeId={}, actorUserId={}", storeId, actorUserId);

        storeService.delete(storeId, actorUserId);
        log.info("[StoreController] delete 성공: storeId={}, actorUserId={}", storeId, actorUserId);
    }
}
