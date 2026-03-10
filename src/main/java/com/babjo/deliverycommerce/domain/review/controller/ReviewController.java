package com.babjo.deliverycommerce.domain.review.controller;

import com.babjo.deliverycommerce.domain.review.dto.*;
import com.babjo.deliverycommerce.domain.review.service.ReviewService;
import com.babjo.deliverycommerce.global.common.dto.CommonResponse;
import com.babjo.deliverycommerce.global.security.CurrentUserResolver;
import com.babjo.deliverycommerce.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 리뷰 컨트롤러
 * - 팀룰: CurrentUserResolver 로 userId 추출
 * - 팀룰: UserPrincipal.getRole() 로 role 추출 후 서비스에 전달
 * - 팀룰: @PreAuthorize 권한 체크
 * - 팀룰: @Valid DTO 검증
 * - 팀룰: CommonResponse 응답 형식
 * - 팀룰: @Tag / @Operation Swagger 문서화
 */
@Tag(name = "Review", description = "리뷰 API")
@RestController
@RequestMapping("/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUserResolver currentUserResolver;

    // ─────────────────────────────────────────────────────────────────
    // POST /v1/reviews  →  리뷰 생성
    // ─────────────────────────────────────────────────────────────────
    @Operation(summary = "리뷰 생성",
            description = "주문 완료 후 가게에 리뷰를 작성합니다. CUSTOMER만 가능.")
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CommonResponse<ReviewCreateResponse>> createReview(
            Authentication authentication,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        Long userId = currentUserResolver.getUserId(authentication);
        return CommonResponse.created("리뷰 생성 성공", reviewService.createReview(userId, request));
    }

    // ─────────────────────────────────────────────────────────────────
    // PUT /v1/reviews/{reviewId}  →  리뷰 수정
    // ─────────────────────────────────────────────────────────────────
    @Operation(summary = "리뷰 수정",
            description = "본인 리뷰 또는 MANAGER·MASTER만 수정 가능. rating·content 선택적 수정.")
    @PutMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'MASTER')")
    public ResponseEntity<CommonResponse<ReviewUpdateResponse>> updateReview(
            Authentication authentication,
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {
        Long userId = currentUserResolver.getUserId(authentication);
        String role = getRole(authentication);
        return CommonResponse.ok("리뷰 수정 성공",
                reviewService.updateReview(userId, role, reviewId, request));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /v1/reviews  →  리뷰 조회 (단건/목록 통합, 페이지네이션)
    // ─────────────────────────────────────────────────────────────────
    @Operation(summary = "리뷰 조회",
            description = "reviewId(단건), storeId(가게별), 파라미터 없음(CUSTOMER:본인 / MANAGER·MASTER:전체). " +
                    "size: 10/30/50 (기본 10), sortBy: createdAt/rating, sortDir: asc/desc")
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'MASTER')")
    public ResponseEntity<CommonResponse<Page<ReviewResponse>>> getReviews(
            Authentication authentication,
            @Valid ReviewSearchRequest searchRequest
    ) {
        Long userId = currentUserResolver.getUserId(authentication);
        String role = getRole(authentication);
        return CommonResponse.ok("리뷰 조회 성공",
                reviewService.getReviews(userId, role, searchRequest));
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE /v1/reviews/{reviewId}  →  리뷰 Soft Delete
    // ─────────────────────────────────────────────────────────────────
    @Operation(summary = "리뷰 삭제",
            description = "본인 리뷰 또는 MANAGER·MASTER만 Soft Delete 가능.")
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'MASTER')")
    public ResponseEntity<CommonResponse<Void>> deleteReview(
            Authentication authentication,
            @PathVariable UUID reviewId
    ) {
        Long userId = currentUserResolver.getUserId(authentication);
        String role = getRole(authentication);
        reviewService.deleteReview(reviewId, userId, role);
        return CommonResponse.ok("리뷰 삭제 성공", null);
    }

    // ─────────────────────────────────────────────────────────────────
    // 헬퍼: 현재 사용자 Role 추출 ("ROLE_MANAGER" 형태)
    // ─────────────────────────────────────────────────────────────────
    private String getRole(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return principal.getRole();
    }
}
