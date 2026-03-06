package com.babjo.deliverycommerce.domain.review.mapper;

import com.babjo.deliverycommerce.domain.review.dto.ReviewCreateRequest;
import com.babjo.deliverycommerce.domain.review.dto.ReviewCreateResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewUpdateResponse;
import com.babjo.deliverycommerce.domain.review.entity.Review;
import com.babjo.deliverycommerce.domain.store.entity.Store;
import com.babjo.deliverycommerce.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .userId(review.getUser().getUserId())
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
            User user,
//            Order order,
            Store store
    ) {
        return Review.create(
                user,
//                order,
                store,
                createRequest.getRating(),
                createRequest.getContent()
        );
    }

    public ReviewCreateResponse toCreateResponse(Review review) {
        return ReviewCreateResponse.builder()
                .reviewId(review.getReviewId())
                .userId(review.getUser().getUserId())
                // [TODO] Order 연결 후 주석 해제
                // .orderId(review.getOrder().getOrderId())
                .storeId(review.getStore().getStoreId())
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .build();
    }

    public ReviewUpdateResponse toUpdateResponse(Review review) {
        return ReviewUpdateResponse.builder()
                .reviewId(review.getReviewId())
                .rating(review.getRating())
                .content(review.getContent())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
