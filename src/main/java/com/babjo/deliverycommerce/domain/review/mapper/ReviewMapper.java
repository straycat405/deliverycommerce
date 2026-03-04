package com.babjo.deliverycommerce.domain.review.mapper;

import com.babjo.deliverycommerce.domain.review.dto.ReviewCreateResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewUpdateResponse;
import com.babjo.deliverycommerce.domain.review.entity.Review;
import com.babjo.deliverycommerce.domain.review.dto.ReviewCreateRequest;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {



    public ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                // [TODO] Order/Store 연결 후 주석 해제 필요
                // .orderId(review.getOrder().getId())
                // .storeId(review.getStore().getId())
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }


    public Review toEntity(
            ReviewCreateRequest createRequest
//            ,User user,
//            Order order,
//            Store store
    ) {
        Review review = new Review();
//        review.setUser(user);
//        review.setOrder(order);
//        review.setStore(store);
        review.setRating(createRequest.getRating());
        review.setContent(createRequest.getContent());
        return review;
    }

    public ReviewCreateResponse toCreateResponse(Review review) {
        ReviewCreateResponse response = new ReviewCreateResponse();
        response.setReviewId(review.getReviewId());
        // [todo] Order/Store 연결 후 주석 해제 필요
        // response.setOrderId(review.getOrder().getId());
        // response.setStoreId(review.getStore().getId());
        response.setRating(review.getRating());
        response.setContent(review.getContent());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }

    public ReviewUpdateResponse toUpdateResponse(Review review) {
        ReviewUpdateResponse response = new ReviewUpdateResponse();
        response.setReviewId(review.getReviewId());
        response.setRating(review.getRating());
        response.setContent(review.getContent());
        response.setUpdatedAt(review.getUpdatedAt());
        return response;
    }
}
