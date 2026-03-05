package com.babjo.deliverycommerce.domain.review.controller;

import java.util.List;
import java.util.UUID;

import com.babjo.deliverycommerce.domain.review.dto.*;
import com.babjo.deliverycommerce.domain.review.service.ReviewService;
import com.babjo.deliverycommerce.global.common.dto.ApiResponse;
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
    public ResponseEntity<ApiResponse<ReviewCreateResponse>> createReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        return ApiResponse.created("리뷰 생성 성공", reviewService.createReview(principal, request));
    }

    @PutMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'MASTER')")
    public ResponseEntity<ApiResponse<ReviewUpdateResponse>> updateReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {
        return ApiResponse.ok("리뷰 수정 성공", reviewService.updateReview(principal, reviewId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'MASTER')")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviews(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID reviewId,
            @RequestParam(required = false) UUID storeId
    ) {
        return ApiResponse.ok("리뷰 조회 성공", reviewService.getReviews(principal, reviewId, storeId));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'MASTER')")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID reviewId
    ) {
        reviewService.deleteReview(reviewId, principal);
        return ApiResponse.ok("리뷰 삭제 성공", null);
    }
}
