package com.babjo.deliverycommerce.review.service;

import com.babjo.deliverycommerce.review.dto.ReviewCreateRequest;
import com.babjo.deliverycommerce.review.dto.ReviewCreateResponse;
import com.babjo.deliverycommerce.review.entity.Review;
import com.babjo.deliverycommerce.review.mapper.ReviewMapper;
import com.babjo.deliverycommerce.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
//         boolean alreadyExists = reviewRepository.existsByOrder(order);
//         if (alreadyExists) {
//             throw new RuntimeException("Review already exists for this order");
//         }

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
}
