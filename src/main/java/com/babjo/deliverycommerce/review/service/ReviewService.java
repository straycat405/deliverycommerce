package com.babjo.deliverycommerce.review.service;

import com.babjo.deliverycommerce.review.dto.ReviewCreateRequest;
import com.babjo.deliverycommerce.review.dto.ReviewCreateResponse;
import com.babjo.deliverycommerce.review.dto.ReviewResponse;
import com.babjo.deliverycommerce.review.dto.ReviewUpdateRequest;
import com.babjo.deliverycommerce.review.dto.ReviewUpdateResponse;
import com.babjo.deliverycommerce.review.entity.Review;
import com.babjo.deliverycommerce.review.mapper.ReviewMapper;
import com.babjo.deliverycommerce.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
// [TODO] 도메인 연결 후 import 추가 필요
// import com.babjo.deliverycommerce.order.entity.Order;
// import com.babjo.deliverycommerce.order.entity.OrderStatus;
// import com.babjo.deliverycommerce.order.repository.OrderRepository;
// import com.babjo.deliverycommerce.store.entity.Store;
// import com.babjo.deliverycommerce.store.repository.StoreRepository;
// import com.babjo.deliverycommerce.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {
    private final ReviewRepository reviewRepository;
//    private final UserRepository userRepository;
//    private final OrderRepository orderRepository;
//    private final StoreRepository storeRepository;
    private final ReviewMapper reviewMapper;

    public ReviewCreateResponse createReview(
//            Long userId,
            ReviewCreateRequest createRequest
    ) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found"));

//        Order order = orderRepository.findById(createRequest.getOrderId())
//                .orElseThrow(() -> new RuntimeException("Order not found"));

        // 주문 상태 COMPLETED 확인 (실패조건: 완료되지 않은 주문)
        // if (order.getStatus() != OrderStatus.COMPLETED) {
        //     throw new RuntimeException("Order is not completed");
        // }

        // 중복 리뷰 방지 (실패조건: 이미 작성된 리뷰)
        // boolean alreadyExists = reviewRepository.existsByOrder(order);
        // if (alreadyExists) {
        //     throw new RuntimeException("Review already exists for this order");
        // }

//        Store store = storeRepository.findById(createRequest.getStoreId())
//                .orElseThrow(() -> new RuntimeException("Store not found"));

        Review review = reviewMapper.toEntity(
                createRequest
//                ,user,
//                order,
//                store
        );
        Review savedReview = reviewRepository.save(review);

        return reviewMapper.toCreateResponse(savedReview);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviews(UUID reviewId, UUID storeId) {
        // [TODO] 인증 연결 후 userId 기반 필터 추가 (아무 파라미터도 않들어왔을때 사용자용 전체 리뷰 조회)

        // reviewId가 있으면 단건 조회 (List 형태로 반환)
        if (reviewId != null) {
            Review review = reviewRepository.findByReviewIdAndDeletedAtIsNull(reviewId)
                    .orElseThrow(() -> new RuntimeException("Review not found"));
            return List.of(reviewMapper.toResponse(review));
        }

        // storeId 필터 목록 조회
        // [TODO] Store 도메인 연결 후 주석 해제
        // if (storeId != null) {
        //     return reviewRepository.findAllByStore_StoreIdAndDeletedAtIsNull(storeId)
        //             .stream()
        //             .map(reviewMapper::toResponse)
        //             .collect(Collectors.toList());
        // }

        // 전체 목록 조회
        return reviewRepository.findAllByDeletedAtIsNull()
                .stream()
                .map(reviewMapper::toResponse)
                .collect(Collectors.toList());
    }

    public void deleteReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // [TODO] 작성자 확인 (실패조건: 작성자 불일치) - 인증 연결 후 주석 해제
        // if (!review.getUser().getId().equals(userId)) {
        //     throw new RuntimeException("Unauthorized: not the author");
        // }

        // [TODO] 인증 연결 후 실제 userId 전달
        review.delete(null);
    }

    public ReviewUpdateResponse updateReview(
//            Long userId,
            UUID reviewId,
            ReviewUpdateRequest updateRequest
    ) {
        // 실패조건: 존재하지 않는 리뷰
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // [TODO] 작성자 확인 (실패조건: 작성자 불일치) - 인증 연결 후 주석 해제
        // if (!review.getUser().getId().equals(userId)) {
        //     throw new RuntimeException("Unauthorized: not the author");
        // }

        // 수정 처리 (updatedAt은 @LastModifiedDate 로 자동 갱신)
        review.setRating(updateRequest.getRating());
        review.setContent(updateRequest.getContent());

        return reviewMapper.toUpdateResponse(review);
    }
}