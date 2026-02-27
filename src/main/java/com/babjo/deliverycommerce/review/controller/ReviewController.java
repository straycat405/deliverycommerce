package com.babjo.deliverycommerce.review.controller;

import java.util.List;
import com.babjo.deliverycommerce.review.dto.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    @PostMapping
    public ReviewCreateResponse createReview(@RequestBody ReviewCreateRequest request) {

        return null;
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
