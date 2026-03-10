package com.babjo.deliverycommerce.domain.store.controller;


import com.babjo.deliverycommerce.domain.store.dto.StoreCreateRequestDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreListResponseDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreResponseDto;
import com.babjo.deliverycommerce.domain.store.dto.StoreUpdateRequestDto;
import com.babjo.deliverycommerce.domain.store.service.StoreService;
import com.babjo.deliverycommerce.global.common.dto.CommonResponse;
import com.babjo.deliverycommerce.global.security.CurrentUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/stores")
public class StoreController {

    private final StoreService storeService;

    // 기존 X-USER-ID 헤더 방식 대신
    // 현재 로그인 사용자 ID를 SecurityContext 기반으로 추출하기 위해 사용
    private final CurrentUserResolver currentUserResolver;

    public StoreController(StoreService storeService, CurrentUserResolver currentUserResolver) {
        this.storeService = storeService;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * 가게 생성
     * - OWNER 권한 사용자만 가능
     * - ownerId는 인증 객체에서 추출한 현재 사용자 ID
     * - 요청 본문은 유효성 검사
     */
    @Operation(
            summary = "가게 생성",
            description = """
                    - OWNER 권한 사용자만 가게를 생성할 수 있습니다.
                    - ownerId는 인증 객체(Authentication)에서 추출합니다.
                    - 요청 본문은 유효성 검사를 수행합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음(OWNER 아님)")
    })
    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CommonResponse<StoreResponseDto>> create(Authentication authentication,
                                                                   @Valid @RequestBody StoreCreateRequestDto request) {

        // 인증 객체에서 현재 로그인 사용자 ID 추출
        // (헤더로 직접 userId를 받지 않고, 공용 인증 구조를 그대로 사용하기 위함)
        Long ownerId = currentUserResolver.getUserId(authentication);

        log.info("가게 생성 API 요청: ownerId={}, category={}, name={}",
                ownerId, request.getCategory(), request.getName());

        StoreResponseDto result = storeService.create(ownerId, request);
        log.info("가게 생성 API 완료: storeId={}, ownerId={}", result.getStoreId(), ownerId);

        return CommonResponse.created("가게 생성 성공", result);
    }

    /**
     * 가게 단건 조회
     */
    @Operation(
            summary = "가게 단건 조회",
            description = """
                    - storeId로 가게 정보를 조회합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 가게")
    })
    @GetMapping("/{storeId}")
    public ResponseEntity<CommonResponse<StoreResponseDto>> get(@PathVariable UUID storeId) {
        log.info("가게 단건 조회 요청: storeId={}", storeId);

        StoreResponseDto result = storeService.get(storeId);
        log.info("가게 단건 조회 완료: storeId={}", storeId);

        return CommonResponse.ok("가게 단건 조회 성공", result);
    }

    /**
     * 가게 수정
     * - OWNER 권한 사용자만 가능
     * - actorUserId는 인증 객체에서 추출한 현재 사용자 ID
     * - 요청 본문은 유효성 검사
     */
    @Operation(
            summary = "가게 수정",
            description = """
                    - OWNER 권한 사용자만 가게를 수정할 수 있습니다.
                    - actorUserId는 인증 객체(Authentication)에서 추출합니다.
                    - 요청 본문은 유효성 검사를 수행합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음(OWNER 아님)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 가게")
    })
    @PatchMapping("/{storeId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CommonResponse<StoreResponseDto>> update(@PathVariable UUID storeId,
                                   Authentication authentication,
                                   @Valid @RequestBody StoreUpdateRequestDto request) {

        Long actorUserId = currentUserResolver.getUserId(authentication);

        log.info("가게 수정 요청: storeId={}, actorUserId={}",
                storeId, actorUserId);

        StoreResponseDto result = storeService.update(storeId, actorUserId, request);
        log.info("가게 수정 완료: storeId={}, actorUserId={}", storeId, actorUserId);

        return CommonResponse.ok("가게 수정 성공", result);
    }

    /**
     * 가게 삭제
     * - OWNER 권한 사용자만 가능
     * - actorUserId는 인증 객체에서 추출
     */
    @Operation(
            summary = "가게 삭제",
            description = """
                    - OWNER 권한 사용자만 가게를 삭제할 수 있습니다.
                    - actorUserId는 인증 객체(Authentication)에서 추출합니다.
                    - 삭제는 Soft Delete 정책을 따릅니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음(OWNER 아님)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 가게")
    })
    @DeleteMapping("/{storeId}")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<CommonResponse<Void>> delete(@PathVariable UUID storeId,
                       Authentication authentication) {

        Long actorUserId = currentUserResolver.getUserId(authentication);

        log.info("가게 삭제 요청: storeId={}, actorUserId={}", storeId, actorUserId);

        storeService.delete(storeId, actorUserId);
        log.info("가게 삭제 완료: storeId={}, actorUserId={}", storeId, actorUserId);

        return CommonResponse.ok("가게 삭제 성공", null);
    }

    /**
     * 가게 목록 조회
     * - category, name은 선택 조건
     * - page 기본값은 0
     */
    @Operation(
            summary = "가게 목록 조회",
            description = """
                    - 가게 목록을 조회합니다.
                    - category, name은 선택 조건입니다.
                    - page 기본값은 0입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<CommonResponse<List<StoreListResponseDto>>> getStores(@RequestParam(required = false) String category,
                                                @RequestParam(required = false) String name,
                                                @RequestParam(defaultValue = "0") int page) {

        log.info("가게 목록 조회 요청 - category={}, name={} page={}", category, name, page);

        List<StoreListResponseDto> response = storeService.getStores(category, name, page);

        log.info("가게 목록 조회 완료 resultCount={}", response.size());

        return CommonResponse.ok("가게 목록 조회 성공", response);
    }
}
