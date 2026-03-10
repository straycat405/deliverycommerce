package com.babjo.deliverycommerce.domain.review.service;

import com.babjo.deliverycommerce.domain.review.dto.ReviewCreateRequest;
import com.babjo.deliverycommerce.domain.review.dto.ReviewCreateResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewSearchRequest;
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
import com.babjo.deliverycommerce.domain.user.entity.User;
import com.babjo.deliverycommerce.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
// [TODO] Order 도메인 연결 후 import 추가 필요
// import com.babjo.deliverycommerce.domain.order.entity.Order;
// import com.babjo.deliverycommerce.domain.order.entity.OrderStatus;
// import com.babjo.deliverycommerce.domain.order.repository.OrderRepository;

/**
 * 리뷰 서비스
 * - 팀룰: 클래스 레벨 @Transactional(readOnly = true), CUD 메서드에 @Transactional 개별 적용
 * - 팀룰: UserPrincipal 직접 의존 없이 userId(Long) + role(String) 파라미터로 처리
 *         (Controller에서 CurrentUserResolver / UserPrincipal.getRole() 추출 후 전달)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
//    private final OrderRepository orderRepository;
    private final ReviewMapper reviewMapper;

    // ─────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public ReviewCreateResponse createReview(Long userId, ReviewCreateRequest createRequest) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

//        Order order = orderRepository.findById(createRequest.getOrderId())
//                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
//        if (order.getStatus() != OrderStatus.COMPLETED) {
//            throw new CustomException(ErrorCode.ORDER_NOT_COMPLETED);
//        }
//        boolean alreadyExists = reviewRepository.existsByOrder(order);
//        if (alreadyExists) {
//            throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
//        }

        Store store = storeRepository.findByStoreIdAndDeletedAtIsNull(createRequest.getStoreId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        Review review = reviewMapper.toEntity(createRequest, user, store);
        Review savedReview = reviewRepository.save(review);

        store.addReview(savedReview.getRating());
        storeRepository.save(store);

        return reviewMapper.toCreateResponse(savedReview);
    }

    // ─────────────────────────────────────────────────────────────────
    // READ / SEARCH
    // ─────────────────────────────────────────────────────────────────

    /**
     * 조건 기반 동적 리뷰 목록 조회 (페이지네이션 + 정렬)
     * - reviewId: 단건 조회 (우선순위 1) — 본인 또는 MANAGER/MASTER
     * - storeId: 가게별 목록 조회 (우선순위 2) — 전체 공개
     * - 파라미터 없음: CUSTOMER → 본인 리뷰만, MANAGER/MASTER → 전체
     *
     * @param userId  로그인 사용자 ID (CurrentUserResolver 추출)
     * @param role    로그인 사용자 Role (UserPrincipal.getRole() 추출) — "ROLE_MANAGER" 형태
     * @param search  검색 조건 (reviewId, storeId, page, size, sortBy, sortDir)
     */
    public Page<ReviewResponse> getReviews(Long userId, String role, ReviewSearchRequest search) {

        // 단건 조회 — reviewId가 존재하면 1건 Page로 래핑해 반환
        if (search.getReviewId() != null) {
            Review review = reviewRepository.findByReviewId(search.getReviewId())
                    .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

            if (!isAdminRole(role) && !review.getUser().getUserId().equals(userId)) {
                throw new CustomException(ErrorCode.REVIEW_FORBIDDEN);
            }

            return new PageImpl<>(
                    List.of(reviewMapper.toResponse(review)),
                    search.toPageable(),
                    1
            );
        }

        // 가게별 목록 조회 — 전체 공개
        if (search.getStoreId() != null) {
            return reviewRepository.findAllByStore_StoreId(search.getStoreId(), search.toPageable())
                    .map(reviewMapper::toResponse);
        }

        // 파라미터 없는 전체 조회
        // MANAGER/MASTER: 전체 리뷰
        // CUSTOMER/OWNER: 본인이 작성한 리뷰만
        if (isAdminRole(role)) {
            return reviewRepository.findAll(search.toPageable())
                    .map(reviewMapper::toResponse);
        }

        return reviewRepository.findAllByUser_UserId(userId, search.toPageable())
                .map(reviewMapper::toResponse);
    }

    // ─────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public ReviewUpdateResponse updateReview(Long userId, String role, UUID reviewId,
                                             ReviewUpdateRequest updateRequest) {
        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUser().getUserId().equals(userId) && !isAdminRole(role)) {
            throw new CustomException(ErrorCode.REVIEW_FORBIDDEN);
        }

        int oldRating = review.getRating();
        review.updateReview(updateRequest.getRating(), updateRequest.getContent());

        Integer newRating = updateRequest.getRating();
        if (newRating != null && oldRating != newRating) {
            Store store = review.getStore();
            store.updateReviewRating(oldRating, newRating);
            storeRepository.save(store);
        }

        return reviewMapper.toUpdateResponse(review);
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE (Soft Delete)
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteReview(UUID reviewId, Long userId, String role) {
        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUser().getUserId().equals(userId) && !isAdminRole(role)) {
            throw new CustomException(ErrorCode.REVIEW_FORBIDDEN);
        }

        Store store = review.getStore();
        store.removeReview(review.getRating());
        storeRepository.save(store);

        review.delete(userId);
        reviewRepository.save(review);
    }

    // ─────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ─────────────────────────────────────────────────────────────────

    /**
     * MANAGER 또는 MASTER 권한 여부 확인
     * role은 "ROLE_MANAGER" 형태 (UserPrincipal.getRole() 반환값)
     */
    private boolean isAdminRole(String role) {
        return UserEnumRole.Authority.MANAGER.equals(role)
                || UserEnumRole.Authority.MASTER.equals(role);
    }
}