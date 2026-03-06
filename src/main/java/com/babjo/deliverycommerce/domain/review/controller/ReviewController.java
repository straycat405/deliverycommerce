package com.babjo.deliverycommerce.domain.review.controller;

import java.util.List;
import java.util.UUID;

import com.babjo.deliverycommerce.domain.review.dto.*;
import com.babjo.deliverycommerce.domain.review.service.ReviewService;
import com.babjo.deliverycommerce.global.common.dto.CommonResponse;
import com.babjo.deliverycommerce.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CommonResponse<ReviewCreateResponse>> createReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        return CommonResponse.created("리뷰 생성 성공", reviewService.createReview(principal, request));
    }

    @PutMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'MASTER')")
    public ResponseEntity<CommonResponse<ReviewUpdateResponse>> updateReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {
        return CommonResponse.ok("리뷰 수정 성공", reviewService.updateReview(principal, reviewId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'MASTER')")
    public ResponseEntity<CommonResponse<List<ReviewResponse>>> getReviews(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID reviewId,  // 우선순위 1: 단건 조회
            @RequestParam(required = false) UUID storeId    // 우선순위 2: 가게별 목록 조회
    ) {
        // 파라미터 우선순위: reviewId > storeId > 전체(MANAGER/MASTER) 또는 본인(CUSTOMER/OWNER)
        return CommonResponse.ok("리뷰 조회 성공", reviewService.getReviews(principal, reviewId, storeId));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'MASTER')")
    public ResponseEntity<CommonResponse<Void>> deleteReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID reviewId
    ) {
        reviewService.deleteReview(reviewId, principal);
        return CommonResponse.ok("리뷰 삭제 성공", null);
    }
}
