package com.babjo.deliverycommerce.review.controller;

import java.util.List;
import com.babjo.deliverycommerce.review.dto.*;
import com.babjo.deliverycommerce.review.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import jakarta.validation.Valid;

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
    public ReviewUpdateResponse updateReview(@PathVariable Long reviewId, @RequestBody ReviewUpdateRequest request) {

        return null;
    }

    @GetMapping
    public List<ReviewResponse> getReviews() {

        return null;
    }

    @DeleteMapping("/{reviewId}")
    public void deleteReview(@PathVariable Long reviewId) {

    }
}
