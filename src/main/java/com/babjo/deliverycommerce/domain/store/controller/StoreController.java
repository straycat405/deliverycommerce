package com.babjo.deliverycommerce.domain.store.controller;


import com.babjo.deliverycommerce.domain.store.dto.StoreCreateRequestDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreListResponseDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreResponseDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreUpdateRequestDto;
import com.babjo.deliverycommerce.domain.store.service.StoreService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    /**
     * 가게 생성
     * - ownerId는 헤더에서 전달
     * - 요청 본문은 유효성 검사
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreResponseDto create(@RequestHeader("X-USER-ID") Long ownerId,
                                   @Valid @RequestBody StoreCreateRequestDto request) {

        log.info("가게 생성 API 요청: ownerId={}, category={}, name={}", ownerId, request.getCategory(), request.getName());

        StoreResponseDto result = storeService.create(ownerId, request);
        log.info("가게 생성 API 완료: storeId={}, ownerId={}", result.getStoreId(), ownerId);

        return result;
    }

    /**
     * 가게 단건 조회
     */
    @GetMapping("/{storeId}")
    public StoreResponseDto get(@PathVariable UUID storeId) {
        log.info("가게 단건 조회 요청: storeId={}", storeId);

        StoreResponseDto result = storeService.get(storeId);
        log.info("가게 단건 조회 완료: storeId={}", storeId);

        return result;
    }

    /**
     * 가게 수정
     * - actorUserId는 헤더에서 전달
     * - 요청 본문은 유효성 검사
     */
    @PatchMapping("/{storeId}")
    public StoreResponseDto update(@PathVariable UUID storeId,
                                   @RequestHeader("X-USER-ID") Long actorUserId,
                                   @Valid @RequestBody StoreUpdateRequestDto request) {

        log.info("가게 수정 요청: storeId={}, actorUserId={}",
                storeId, actorUserId);

        StoreResponseDto result = storeService.update(storeId, actorUserId, request);
        log.info("가게 수정 완료: storeId={}, actorUserId={}", storeId, actorUserId);

        return result;
    }

    /**
     * 가게 삭제
     * - actorUserId는 헤더에서 전달
     */
    @DeleteMapping("/{storeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID storeId,
                       @RequestHeader("X-USER-ID") Long actorUserId) {

        log.info("가게 삭제 요청: storeId={}, actorUserId={}", storeId, actorUserId);

        storeService.delete(storeId, actorUserId);
        log.info("가게 삭제 완료: storeId={}, actorUserId={}", storeId, actorUserId);
    }

    /**
     * 가게 목록 조회
     * - category, name은 선택 조건
     * - page 기본값은 0
     */
    @GetMapping
    public List<StoreListResponseDto> getStores(@RequestParam(required = false) String category,
                                                @RequestParam(required = false) String name,
                                                @RequestParam(defaultValue = "0") int page) {

        log.info("가게 목록 조회 요청 - category={}, name={} page={}", category, name, page);

        List<StoreListResponseDto> response = storeService.getStores(category, name, page);

        log.info("가게 목록 조회 완료 resultCount={}", response.size());

        return response;
    }
}
