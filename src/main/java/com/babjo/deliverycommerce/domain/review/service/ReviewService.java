package com.babjo.deliverycommerce.domain.review.service;

import com.babjo.deliverycommerce.domain.review.dto.ReviewCreateRequest;
import com.babjo.deliverycommerce.domain.review.dto.ReviewCreateResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewUpdateRequest;
import com.babjo.deliverycommerce.domain.review.dto.ReviewUpdateResponse;
import com.babjo.deliverycommerce.domain.review.entity.Review;
import com.babjo.deliverycommerce.domain.review.mapper.ReviewMapper;
import com.babjo.deliverycommerce.domain.review.repository.ReviewRepository;
import com.babjo.deliverycommerce.domain.store.entity.Store;
import com.babjo.deliverycommerce.domain.store.repository.StoreRepository;
import com.babjo.deliverycommerce.global.common.enums.UserEnumRole;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.babjo.deliverycommerce.global.security.UserPrincipal;
import com.babjo.deliverycommerce.domain.user.entity.User;
import com.babjo.deliverycommerce.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
// [TODO] Order 도메인 연결 후 import 추가 필요
// import com.babjo.deliverycommerce.domain.order.entity.Order;
// import com.babjo.deliverycommerce.domain.order.entity.OrderStatus;
// import com.babjo.deliverycommerce.domain.order.repository.OrderRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
//    private final OrderRepository orderRepository;
    private final ReviewMapper reviewMapper;

    @Transactional
    public ReviewCreateResponse createReview(
            UserPrincipal principal,
            ReviewCreateRequest createRequest
    ) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

//        Order order = orderRepository.findById(createRequest.getOrderId())
//                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 주문 상태 COMPLETED 확인 (실패조건: 완료되지 않은 주문)
        // if (order.getStatus() != OrderStatus.COMPLETED) {
        //     throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
        // }

        // 중복 리뷰 방지 - 1주문 1리뷰 (실패조건: 해당 주문에 이미 리뷰 존재)
        // [TODO] Order 연결 후 주석 해제
        // boolean alreadyExists = reviewRepository.existsByOrderAndDeletedAtIsNull(order);
        // if (alreadyExists) {
        //     throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
        // }

        Store store = storeRepository.findByStoreIdAndDeletedAtIsNull(createRequest.getStoreId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        Review review = reviewMapper.toEntity(
                createRequest,
                user,
//                order,
                store
        );
        Review savedReview = reviewRepository.save(review);

        store.addReview(savedReview.getRating());
        storeRepository.save(store);

        return reviewMapper.toCreateResponse(savedReview);
    }

    public List<ReviewResponse> getReviews(UserPrincipal principal, UUID reviewId, UUID storeId) {
        // reviewId 단건 조회 — 본인 리뷰이거나 MANAGER/MASTER만 접근 가능
        if (reviewId != null) {
            Review review = reviewRepository.findByReviewId(reviewId)
                    .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

            boolean isAdminRole = isAdminRole(principal);
            if (!isAdminRole && !review.getUser().getUserId().equals(principal.getUserId())) {
                throw new CustomException(ErrorCode.REVIEW_FORBIDDEN);
            }

            return List.of(reviewMapper.toResponse(review));
        }

        // storeId 필터 조회 — 해당 가게의 리뷰 목록 (전체 공개)
        if (storeId != null) {
            return reviewRepository.findAllByStore_StoreId(storeId)
                    .stream()
                    .map(reviewMapper::toResponse)
                    .collect(Collectors.toList());
        }

        // 파라미터 없는 전체 조회
        // CUSTOMER: 본인이 작성한 리뷰만 반환
        // MANAGER / MASTER: 전체 리뷰 반환
        if (isAdminRole(principal)) {
            return reviewRepository.findAll()
                    .stream()
                    .map(reviewMapper::toResponse)
                    .collect(Collectors.toList());
        }

        return reviewRepository.findAllByUser_UserId(principal.getUserId())
                .stream()
                .map(reviewMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteReview(UUID reviewId, UserPrincipal principal) {
        // @Where(deleted_at IS NULL) 적용으로 이미 삭제된 리뷰는 REVIEW_NOT_FOUND 반환
        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        // 작성자 또는 MANAGER/MASTER만 삭제 가능
        if (!review.getUser().getUserId().equals(principal.getUserId()) && !isAdminRole(principal)) {
            throw new CustomException(ErrorCode.REVIEW_FORBIDDEN);
        }

        Store store = review.getStore();
        store.removeReview(review.getRating());
        storeRepository.save(store);

        review.delete(principal.getUserId());
        reviewRepository.save(review);
    }

    @Transactional
    public ReviewUpdateResponse updateReview(
            UserPrincipal principal,
            UUID reviewId,
            ReviewUpdateRequest updateRequest
    ) {
        // 실패조건: 존재하지 않는 리뷰
        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        // 작성자 또는 MANAGER/MASTER만 수정 가능
        if (!review.getUser().getUserId().equals(principal.getUserId()) && !isAdminRole(principal)) {
            throw new CustomException(ErrorCode.REVIEW_FORBIDDEN);
        }

        int oldRating = review.getRating();

        // 수정 처리 (updatedAt은 @LastModifiedDate 로 자동 갱신)
        review.updateReview(updateRequest.getRating(), updateRequest.getContent());

        // 별점이 변경된 경우에만 Store 통계 갱신
        Integer newRating = updateRequest.getRating();
        if (newRating != null && oldRating != newRating) {
            Store store = review.getStore();
            store.updateReviewRating(oldRating, newRating);
            storeRepository.save(store);
        }

        return reviewMapper.toUpdateResponse(review);
    }

    /**
     * MANAGER 또는 MASTER 권한 여부 확인
     * UserPrincipal의 role은 "ROLE_MANAGER" 형태이므로 Authority 상수와 비교합니다.
     */
    private boolean isAdminRole(UserPrincipal principal) {
        String role = principal.getRole();
        return UserEnumRole.Authority.MANAGER.equals(role) || UserEnumRole.Authority.MASTER.equals(role);
    }
}