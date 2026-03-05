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
import com.babjo.deliverycommerce.user.entity.User;
import com.babjo.deliverycommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewService reviewService;

    private UserPrincipal principal;        // userId=1L, CUSTOMER
    private UserPrincipal otherPrincipal;   // userId=2L, CUSTOMER (다른 작성자)
    private UserPrincipal managerPrincipal; // userId=3L, MANAGER
    private Store store;
    private User user;
    private Review review;

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal(1L, "testuser", "CUSTOMER");
        otherPrincipal = new UserPrincipal(2L, "otheruser", "CUSTOMER");
        managerPrincipal = new UserPrincipal(3L, "manager", "MANAGER");

        store = Store.create(1L, "한식", "테스트 식당", "서울시 강남구");
        user = User.createForTest(1L, "testuser", "test@test.com", "테스터", UserEnumRole.CUSTOMER);

        review = Review.create(user, store, 4, "맛있어요");
    }

    // ───────────────────────────────────────────────
    // createReview
    // ───────────────────────────────────────────────

    @Test
    void createReview_성공() {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest();

        ReviewCreateResponse expectedResponse = new ReviewCreateResponse();
        expectedResponse.setRating(4);
        expectedResponse.setContent("맛있어요");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(any())).willReturn(Optional.of(store));
        given(reviewMapper.toEntity(any(), any(), any())).willReturn(review);
        given(reviewRepository.save(any())).willReturn(review);
        given(reviewMapper.toCreateResponse(any())).willReturn(expectedResponse);

        // when
        ReviewCreateResponse result = reviewService.createReview(principal, request);

        // then
        assertThat(result.getRating()).isEqualTo(4);
        assertThat(result.getContent()).isEqualTo("맛있어요");
        verify(reviewRepository, times(1)).save(any());
    }

    @Test
    void createReview_성공_store_통계_갱신됨() {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest();

        ReviewCreateResponse expectedResponse = new ReviewCreateResponse();
        expectedResponse.setRating(4);
        expectedResponse.setContent("맛있어요");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(any())).willReturn(Optional.of(store));
        given(reviewMapper.toEntity(any(), any(), any())).willReturn(review);
        given(reviewRepository.save(any())).willReturn(review);
        given(reviewMapper.toCreateResponse(any())).willReturn(expectedResponse);

        // when
        reviewService.createReview(principal, request);

        // then — store 통계가 갱신되고 저장되어야 함
        assertThat(store.getReviewCount()).isEqualTo(1);
        assertThat(store.getAverageRating()).isEqualTo(4.0);
        verify(storeRepository, times(1)).save(store);
    }

    @Test
    void createReview_실패_존재하지_않는_유저() {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest();
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(principal, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_실패_존재하지_않는_가게() {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(principal, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.STORE_NOT_FOUND));

        verify(reviewRepository, never()).save(any());
    }

    // ───────────────────────────────────────────────
    // getReviews
    // ───────────────────────────────────────────────

    @Test
    void getReviews_reviewId_단건조회_성공() {
        // given
        UUID reviewId = UUID.randomUUID();
        ReviewResponse expectedResponse = ReviewResponse.builder()
                .reviewId(reviewId)
                .userId(1L)
                .rating(4)
                .content("맛있어요")
                .build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toResponse(review)).willReturn(expectedResponse);

        // when
        List<ReviewResponse> result = reviewService.getReviews(reviewId, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReviewId()).isEqualTo(reviewId);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
        verify(reviewRepository, times(1)).findByReviewId(reviewId);
        verify(reviewRepository, never()).findAllByStore_StoreId(any());
        verify(reviewRepository, never()).findAll();
    }

    @Test
    void getReviews_reviewId_단건조회_실패_존재하지_않는_리뷰() {
        // given
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.getReviews(reviewId, null))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
    }

    @Test
    void getReviews_storeId_필터_목록조회_성공() {
        // given
        UUID storeId = UUID.randomUUID();
        ReviewResponse reviewResponse = ReviewResponse.builder()
                .storeId(storeId)
                .userId(1L)
                .rating(3)
                .content("보통이에요")
                .build();

        given(reviewRepository.findAllByStore_StoreId(storeId)).willReturn(List.of(review));
        given(reviewMapper.toResponse(review)).willReturn(reviewResponse);

        // when
        List<ReviewResponse> result = reviewService.getReviews(null, storeId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStoreId()).isEqualTo(storeId);
        verify(reviewRepository, times(1)).findAllByStore_StoreId(storeId);
        verify(reviewRepository, never()).findAll();
    }

    @Test
    void getReviews_storeId_필터_결과없음_빈목록_반환() {
        // given
        UUID storeId = UUID.randomUUID();
        given(reviewRepository.findAllByStore_StoreId(storeId)).willReturn(List.of());

        // when
        List<ReviewResponse> result = reviewService.getReviews(null, storeId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void getReviews_전체조회_성공() {
        // given
        ReviewResponse reviewResponse = ReviewResponse.builder()
                .userId(1L)
                .rating(4)
                .content("맛있어요")
                .build();

        given(reviewRepository.findAll()).willReturn(List.of(review));
        given(reviewMapper.toResponse(review)).willReturn(reviewResponse);

        // when
        List<ReviewResponse> result = reviewService.getReviews(null, null);

        // then
        assertThat(result).hasSize(1);
        verify(reviewRepository, times(1)).findAll();
    }

    @Test
    void getReviews_파라미터_없음_전체조회_실행() {
        // given
        given(reviewRepository.findAll()).willReturn(List.of());

        // when
        List<ReviewResponse> result = reviewService.getReviews(null, null);

        // then
        assertThat(result).isEmpty();
        verify(reviewRepository, times(1)).findAll();
        verify(reviewRepository, never()).findByReviewId(any());
        verify(reviewRepository, never()).findAllByStore_StoreId(any());
    }

    // ───────────────────────────────────────────────
    // updateReview
    // ───────────────────────────────────────────────

    @Test
    void updateReview_성공() {
        // given
        UUID reviewId = UUID.randomUUID();

        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);
        given(request.getRating()).willReturn(5);
        given(request.getContent()).willReturn("정말 맛있어요");

        ReviewUpdateResponse expectedResponse = new ReviewUpdateResponse();
        expectedResponse.setReviewId(reviewId);
        expectedResponse.setRating(5);
        expectedResponse.setContent("정말 맛있어요");

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toUpdateResponse(review)).willReturn(expectedResponse);

        // when
        ReviewUpdateResponse result = reviewService.updateReview(principal, reviewId, request);

        // then
        assertThat(result.getReviewId()).isEqualTo(reviewId);
        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getContent()).isEqualTo("정말 맛있어요");
    }

    @Test
    void updateReview_실패_작성자_불일치() {
        // given
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);

        // review의 작성자는 userId=1L, otherPrincipal은 userId=2L
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(otherPrincipal, reviewId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_FORBIDDEN));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void updateReview_성공_MANAGER는_타인_리뷰_수정_가능() {
        // given
        UUID reviewId = UUID.randomUUID();

        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);
        given(request.getRating()).willReturn(3);
        given(request.getContent()).willReturn("관리자 수정");

        ReviewUpdateResponse expectedResponse = new ReviewUpdateResponse();
        expectedResponse.setReviewId(reviewId);
        expectedResponse.setRating(3);
        expectedResponse.setContent("관리자 수정");

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toUpdateResponse(review)).willReturn(expectedResponse);

        // when
        ReviewUpdateResponse result = reviewService.updateReview(managerPrincipal, reviewId, request);

        // then
        assertThat(result.getRating()).isEqualTo(3);
    }

    @Test
    void updateReview_별점_변경_시_store_통계_갱신됨() {
        // given
        UUID reviewId = UUID.randomUUID();
        store.addReview(4); // count=1, avg=4.0

        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);
        given(request.getRating()).willReturn(2); // 4 → 2로 변경
        given(request.getContent()).willReturn("별로에요");

        ReviewUpdateResponse expectedResponse = new ReviewUpdateResponse();
        expectedResponse.setReviewId(reviewId);
        expectedResponse.setRating(2);
        expectedResponse.setContent("별로에요");

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toUpdateResponse(review)).willReturn(expectedResponse);

        // when
        reviewService.updateReview(principal, reviewId, request);

        // then — 4 → 2 변경: avg = 4.0 + (2 - 4) / 1 = 2.0
        assertThat(store.getReviewCount()).isEqualTo(1);
        assertThat(store.getAverageRating()).isEqualTo(2.0);
        verify(storeRepository, times(1)).save(store);
    }

    @Test
    void updateReview_별점_동일하면_store_저장_생략() {
        // given
        UUID reviewId = UUID.randomUUID();

        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);
        given(request.getRating()).willReturn(4);  // oldRating == newRating
        given(request.getContent()).willReturn("내용만 수정");

        ReviewUpdateResponse expectedResponse = new ReviewUpdateResponse();
        expectedResponse.setReviewId(reviewId);
        expectedResponse.setRating(4);
        expectedResponse.setContent("내용만 수정");

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toUpdateResponse(review)).willReturn(expectedResponse);

        // when
        reviewService.updateReview(principal, reviewId, request);

        // then — 별점 변경 없으므로 storeRepository.save 호출되지 않아야 함
        verify(storeRepository, never()).save(any());
    }

    @Test
    void updateReview_실패_존재하지_않는_리뷰() {
        // given
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(principal, reviewId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
    }

    // ───────────────────────────────────────────────
    // deleteReview
    // ───────────────────────────────────────────────

    @Test
    void deleteReview_성공() {
        // given
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        // when
        reviewService.deleteReview(reviewId, principal);

        // then
        assertThat(review.getDeletedAt()).isNotNull();
        assertThat(review.getDeletedBy()).isEqualTo(principal.getUserId());
        verify(reviewRepository, times(1)).save(review);
    }

    @Test
    void deleteReview_실패_작성자_불일치() {
        // given
        UUID reviewId = UUID.randomUUID();
        // review의 작성자는 userId=1L, otherPrincipal은 userId=2L
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, otherPrincipal))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_FORBIDDEN));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void deleteReview_성공_MANAGER는_타인_리뷰_삭제_가능() {
        // given
        UUID reviewId = UUID.randomUUID();
        store.addReview(4);
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        // when
        reviewService.deleteReview(reviewId, managerPrincipal);

        // then
        assertThat(review.getDeletedAt()).isNotNull();
        assertThat(review.getDeletedBy()).isEqualTo(3L);
    }

    @Test
    void deleteReview_성공_store_통계_갱신됨() {
        // given
        UUID reviewId = UUID.randomUUID();
        store.addReview(4); // 미리 1개 등록 (count=1, avg=4.0)

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        // when
        reviewService.deleteReview(reviewId, principal);

        // then — 삭제 후 count=0, avg=0.0
        assertThat(store.getReviewCount()).isEqualTo(0);
        assertThat(store.getAverageRating()).isEqualTo(0.0);
        verify(storeRepository, times(1)).save(store);
    }

    @Test
    void deleteReview_실패_존재하지_않는_리뷰() {
        // given
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, principal))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_NOT_FOUND));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void deleteReview_이미_삭제된_리뷰는_중복_삭제되지_않음() {
        // given
        UUID reviewId = UUID.randomUUID();
        review.delete(principal.getUserId()); // 미리 삭제 처리
        var firstDeletedAt = review.getDeletedAt();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        // when
        reviewService.deleteReview(reviewId, principal);

        // then — BaseEntity.delete()의 idempotent 보장 — deletedAt이 최초 값 유지
        assertThat(review.getDeletedAt()).isEqualTo(firstDeletedAt);
        verify(reviewRepository, times(1)).save(review);
    }
}

