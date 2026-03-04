package com.babjo.deliverycommerce.domain.review.mapper;

import com.babjo.deliverycommerce.domain.review.dto.ReviewCreateRequest;
import com.babjo.deliverycommerce.domain.review.dto.ReviewCreateResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewUpdateResponse;
import com.babjo.deliverycommerce.domain.review.entity.Review;
import com.babjo.deliverycommerce.domain.store.entity.Store;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                // [TODO] Order 연결 후 주석 해제
                // .orderId(review.getOrder().getOrderId())
                .storeId(review.getStore().getStoreId())
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    public Review toEntity(
            ReviewCreateRequest createRequest,
//            User user,
//            Order order,
            Store store
    ) {
        return Review.create(
//                user,
//                order,
                store,
                createRequest.getRating(),
                createRequest.getContent()
        );
    }

    public ReviewCreateResponse toCreateResponse(Review review) {
        ReviewCreateResponse response = new ReviewCreateResponse();
        response.setReviewId(review.getReviewId());
        // [TODO] Order 연결 후 주석 해제
        // response.setOrderId(review.getOrder().getOrderId());
        response.setStoreId(review.getStore().getStoreId());
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
