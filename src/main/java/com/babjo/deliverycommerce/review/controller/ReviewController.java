package com.babjo.deliverycommerce.review.controller;

import java.util.List;
import java.util.UUID;
import com.babjo.deliverycommerce.review.dto.*;
import com.babjo.deliverycommerce.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ReviewCreateResponse createReview(
//            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid
            @RequestBody ReviewCreateRequest request
    ) {
        return reviewService.createReview(
//                userDetails.getUserId(),
                request
        );
    }

    @PutMapping("/{reviewId}")
    public ReviewUpdateResponse updateReview(
//            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID reviewId,
            @Valid
            @RequestBody ReviewUpdateRequest request
    ) {
        return reviewService.updateReview(
//                userDetails.getUserId(),
                reviewId,
                request
        );
    }

    @GetMapping
    public List<ReviewResponse> getReviews(
            @RequestParam(required = false) UUID reviewId,
            @RequestParam(required = false) UUID storeId
    ) {
        return reviewService.getReviews(reviewId, storeId);
    }

    @DeleteMapping("/{reviewId}")
    public void deleteReview(@PathVariable UUID reviewId) {
        reviewService.deleteReview(reviewId);
    }
}
