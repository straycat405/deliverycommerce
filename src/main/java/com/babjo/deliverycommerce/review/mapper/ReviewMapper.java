package com.babjo.deliverycommerce.review.mapper;

import com.babjo.deliverycommerce.review.dto.ReviewCreateResponse;
import com.babjo.deliverycommerce.review.dto.ReviewUpdateResponse;
import com.babjo.deliverycommerce.review.entity.Review;
import com.babjo.deliverycommerce.review.dto.ReviewCreateRequest;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

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
