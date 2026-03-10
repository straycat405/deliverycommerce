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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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

    // 팀룰: 서비스는 UserPrincipal 없이 userId(Long) + role(String) 으로 동작
    private static final Long    CUSTOMER_ID  = 1L;
    private static final String  CUSTOMER_ROLE = "ROLE_CUSTOMER";

    private static final Long    OTHER_ID     = 2L;
    private static final String  OTHER_ROLE   = "ROLE_CUSTOMER";

    private static final Long    MANAGER_ID   = 3L;
    private static final String  MANAGER_ROLE = "ROLE_MANAGER";

    private static final Long    MASTER_ID    = 4L;
    private static final String  MASTER_ROLE  = "ROLE_MASTER";

    private static final Long    OWNER_ID     = 5L;
    private static final String  OWNER_ROLE   = "ROLE_OWNER";

    private Store  store;
    private User   user;
    private Review review;

    /** 기본 ReviewSearchRequest (page=0, size=10, sortBy=createdAt, sortDir=desc) */
    private ReviewSearchRequest defaultSearch() {
        return new ReviewSearchRequest();
    }

    /** reviewId 조건이 있는 ReviewSearchRequest */
    private ReviewSearchRequest searchByReviewId(UUID reviewId) {
        ReviewSearchRequest s = new ReviewSearchRequest();
        s.setReviewId(reviewId);
        return s;
    }

    /** storeId 조건이 있는 ReviewSearchRequest */
    private ReviewSearchRequest searchByStoreId(UUID storeId) {
        ReviewSearchRequest s = new ReviewSearchRequest();
        s.setStoreId(storeId);
        return s;
    }

    @BeforeEach
    void setUp() {
        store  = Store.create(1L, "한식", "테스트 식당", "서울시 강남구");
        user   = User.createForTest(1L, "testuser", "test@test.com", "테스터", UserEnumRole.CUSTOMER);
        review = Review.create(user, store, 4, "맛있어요");
    }

    // ─────────────────────────────────────────────────────────────────
    // createReview
    // ─────────────────────────────────────────────────────────────────

    @Test
    void createReview_성공() {
        ReviewCreateRequest request = new ReviewCreateRequest();
        ReviewCreateResponse expectedResponse = ReviewCreateResponse.builder()
                .rating(4).content("맛있어요").build();

        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(user));
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(any())).willReturn(Optional.of(store));
        given(reviewMapper.toEntity(any(), any(), any())).willReturn(review);
        given(reviewRepository.save(any())).willReturn(review);
        given(reviewMapper.toCreateResponse(any())).willReturn(expectedResponse);

        ReviewCreateResponse result = reviewService.createReview(CUSTOMER_ID, request);

        assertThat(result.getRating()).isEqualTo(4);
        assertThat(result.getContent()).isEqualTo("맛있어요");
        verify(reviewRepository, times(1)).save(any());
    }

    @Test
    void createReview_성공_store_통계_갱신됨() {
        ReviewCreateRequest request = new ReviewCreateRequest();
        ReviewCreateResponse expectedResponse = ReviewCreateResponse.builder()
                .rating(4).content("맛있어요").build();

        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(user));
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(any())).willReturn(Optional.of(store));
        given(reviewMapper.toEntity(any(), any(), any())).willReturn(review);
        given(reviewRepository.save(any())).willReturn(review);
        given(reviewMapper.toCreateResponse(any())).willReturn(expectedResponse);

        reviewService.createReview(CUSTOMER_ID, request);

        assertThat(store.getReviewCount()).isEqualTo(1);
        assertThat(store.getAverageRating()).isEqualTo(4.0);
        verify(storeRepository, times(1)).save(store);
    }

    @Test
    void createReview_실패_존재하지_않는_유저() {
        ReviewCreateRequest request = new ReviewCreateRequest();
        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(CUSTOMER_ID, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_실패_존재하지_않는_가게() {
        ReviewCreateRequest request = new ReviewCreateRequest();
        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(user));
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(CUSTOMER_ID, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.STORE_NOT_FOUND));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_실패_소프트삭제된_가게() {
        ReviewCreateRequest request = new ReviewCreateRequest();
        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(user));
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(CUSTOMER_ID, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.STORE_NOT_FOUND));

        verify(storeRepository, never()).save(any());
        verify(reviewRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────
    // getReviews (단건 조회 — reviewId 조건)
    // ─────────────────────────────────────────────────────────────────

    @Test
    void getReviews_CUSTOMER_본인_리뷰_단건조회_성공() {
        UUID reviewId = UUID.randomUUID();
        ReviewResponse expectedResponse = ReviewResponse.builder()
                .reviewId(reviewId).userId(CUSTOMER_ID).rating(4).content("맛있어요").build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toResponse(review)).willReturn(expectedResponse);

        Page<ReviewResponse> result = reviewService.getReviews(CUSTOMER_ID, CUSTOMER_ROLE, searchByReviewId(reviewId));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getReviewId()).isEqualTo(reviewId);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(CUSTOMER_ID);
        verify(reviewRepository, times(1)).findByReviewId(reviewId);
    }

    @Test
    void getReviews_CUSTOMER_타인_리뷰_단건조회_실패() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        // review 작성자 userId=1L, 요청자 OTHER_ID=2L
        assertThatThrownBy(() -> reviewService.getReviews(OTHER_ID, OTHER_ROLE, searchByReviewId(reviewId)))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_FORBIDDEN));
    }

    @Test
    void getReviews_MANAGER_타인_리뷰_단건조회_성공() {
        UUID reviewId = UUID.randomUUID();
        ReviewResponse expectedResponse = ReviewResponse.builder()
                .reviewId(reviewId).userId(CUSTOMER_ID).rating(4).content("맛있어요").build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toResponse(review)).willReturn(expectedResponse);

        Page<ReviewResponse> result = reviewService.getReviews(MANAGER_ID, MANAGER_ROLE, searchByReviewId(reviewId));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void getReviews_MASTER_타인_리뷰_단건조회_성공() {
        UUID reviewId = UUID.randomUUID();
        ReviewResponse expectedResponse = ReviewResponse.builder()
                .reviewId(reviewId).userId(CUSTOMER_ID).rating(4).content("맛있어요").build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toResponse(review)).willReturn(expectedResponse);

        Page<ReviewResponse> result = reviewService.getReviews(MASTER_ID, MASTER_ROLE, searchByReviewId(reviewId));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getReviews_단건조회_실패_존재하지_않는_리뷰() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviews(CUSTOMER_ID, CUSTOMER_ROLE, searchByReviewId(reviewId)))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
    }

    // ─────────────────────────────────────────────────────────────────
    // getReviews (가게별 목록 조회 — storeId 조건)
    // ─────────────────────────────────────────────────────────────────

    @Test
    void getReviews_storeId_필터_목록조회_성공_CUSTOMER() {
        UUID storeId = UUID.randomUUID();
        ReviewResponse reviewResponse = ReviewResponse.builder()
                .storeId(storeId).userId(CUSTOMER_ID).rating(3).content("보통이에요").build();

        Page<Review> reviewPage = new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1);
        given(reviewRepository.findAllByStore_StoreId(eq(storeId), any())).willReturn(reviewPage);
        given(reviewMapper.toResponse(review)).willReturn(reviewResponse);

        Page<ReviewResponse> result = reviewService.getReviews(CUSTOMER_ID, CUSTOMER_ROLE, searchByStoreId(storeId));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStoreId()).isEqualTo(storeId);
        verify(reviewRepository, times(1)).findAllByStore_StoreId(eq(storeId), any());
    }

    @Test
    void getReviews_storeId_필터_결과없음_빈목록_반환() {
        UUID storeId = UUID.randomUUID();
        Page<Review> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        given(reviewRepository.findAllByStore_StoreId(eq(storeId), any())).willReturn(emptyPage);

        Page<ReviewResponse> result = reviewService.getReviews(CUSTOMER_ID, CUSTOMER_ROLE, searchByStoreId(storeId));

        assertThat(result.getContent()).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────
    // getReviews (파라미터 없음 — 권한별 전체/본인)
    // ─────────────────────────────────────────────────────────────────

    @Test
    void getReviews_파라미터없음_CUSTOMER_본인_리뷰만_반환() {
        ReviewResponse reviewResponse = ReviewResponse.builder()
                .userId(CUSTOMER_ID).rating(4).content("맛있어요").build();

        Page<Review> reviewPage = new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1);
        given(reviewRepository.findAllByUser_UserId(eq(CUSTOMER_ID), any())).willReturn(reviewPage);
        given(reviewMapper.toResponse(review)).willReturn(reviewResponse);

        Page<ReviewResponse> result = reviewService.getReviews(CUSTOMER_ID, CUSTOMER_ROLE, defaultSearch());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(CUSTOMER_ID);
        verify(reviewRepository, times(1)).findAllByUser_UserId(eq(CUSTOMER_ID), any());
        verify(reviewRepository, never()).findAll(any());
    }

    @Test
    void getReviews_파라미터없음_CUSTOMER_본인_리뷰_없으면_빈목록() {
        Page<Review> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        given(reviewRepository.findAllByUser_UserId(eq(CUSTOMER_ID), any())).willReturn(emptyPage);

        Page<ReviewResponse> result = reviewService.getReviews(CUSTOMER_ID, CUSTOMER_ROLE, defaultSearch());

        assertThat(result.getContent()).isEmpty();
        verify(reviewRepository, times(1)).findAllByUser_UserId(eq(CUSTOMER_ID), any());
        verify(reviewRepository, never()).findAll(any());
    }

    @Test
    void getReviews_파라미터없음_OWNER_본인_리뷰만_반환() {
        Page<Review> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        given(reviewRepository.findAllByUser_UserId(eq(OWNER_ID), any())).willReturn(emptyPage);

        Page<ReviewResponse> result = reviewService.getReviews(OWNER_ID, OWNER_ROLE, defaultSearch());

        assertThat(result.getContent()).isEmpty();
        verify(reviewRepository, times(1)).findAllByUser_UserId(eq(OWNER_ID), any());
        verify(reviewRepository, never()).findAll(any());
    }

    @Test
    void getReviews_파라미터없음_MANAGER_전체_리뷰_반환() {
        ReviewResponse reviewResponse = ReviewResponse.builder()
                .userId(CUSTOMER_ID).rating(4).content("맛있어요").build();

        Page<Review> reviewPage = new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1);
        given(reviewRepository.findAll(any(org.springframework.data.domain.Pageable.class))).willReturn(reviewPage);
        given(reviewMapper.toResponse(review)).willReturn(reviewResponse);

        Page<ReviewResponse> result = reviewService.getReviews(MANAGER_ID, MANAGER_ROLE, defaultSearch());

        assertThat(result.getContent()).hasSize(1);
        verify(reviewRepository, times(1)).findAll(any(org.springframework.data.domain.Pageable.class));
        verify(reviewRepository, never()).findAllByUser_UserId(any(), any());
    }

    @Test
    void getReviews_파라미터없음_MASTER_전체_리뷰_반환() {
        Page<Review> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        given(reviewRepository.findAll(any(org.springframework.data.domain.Pageable.class))).willReturn(emptyPage);

        Page<ReviewResponse> result = reviewService.getReviews(MASTER_ID, MASTER_ROLE, defaultSearch());

        assertThat(result.getContent()).isEmpty();
        verify(reviewRepository, times(1)).findAll(any(org.springframework.data.domain.Pageable.class));
        verify(reviewRepository, never()).findAllByUser_UserId(any(), any());
    }

    // ─────────────────────────────────────────────────────────────────
    // updateReview
    // ─────────────────────────────────────────────────────────────────

    @Test
    void updateReview_성공() {
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);
        given(request.getRating()).willReturn(5);
        given(request.getContent()).willReturn("정말 맛있어요");

        ReviewUpdateResponse expectedResponse = ReviewUpdateResponse.builder()
                .reviewId(reviewId).rating(5).content("정말 맛있어요").build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toUpdateResponse(review)).willReturn(expectedResponse);

        ReviewUpdateResponse result = reviewService.updateReview(CUSTOMER_ID, CUSTOMER_ROLE, reviewId, request);

        assertThat(result.getReviewId()).isEqualTo(reviewId);
        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getContent()).isEqualTo("정말 맛있어요");
    }

    @Test
    void updateReview_실패_작성자_불일치() {
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        // review 작성자 userId=1L, OTHER_ID=2L
        assertThatThrownBy(() -> reviewService.updateReview(OTHER_ID, OTHER_ROLE, reviewId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_FORBIDDEN));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void updateReview_성공_MANAGER는_타인_리뷰_수정_가능() {
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);
        given(request.getRating()).willReturn(3);
        given(request.getContent()).willReturn("관리자 수정");

        ReviewUpdateResponse expectedResponse = ReviewUpdateResponse.builder()
                .reviewId(reviewId).rating(3).content("관리자 수정").build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toUpdateResponse(review)).willReturn(expectedResponse);

        ReviewUpdateResponse result = reviewService.updateReview(MANAGER_ID, MANAGER_ROLE, reviewId, request);

        assertThat(result.getRating()).isEqualTo(3);
    }

    @Test
    void updateReview_성공_MASTER는_타인_리뷰_수정_가능() {
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);
        given(request.getRating()).willReturn(2);
        given(request.getContent()).willReturn("마스터 수정");

        ReviewUpdateResponse expectedResponse = ReviewUpdateResponse.builder()
                .reviewId(reviewId).rating(2).content("마스터 수정").build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toUpdateResponse(review)).willReturn(expectedResponse);

        ReviewUpdateResponse result = reviewService.updateReview(MASTER_ID, MASTER_ROLE, reviewId, request);

        assertThat(result.getRating()).isEqualTo(2);
    }

    @Test
    void updateReview_실패_OWNER는_타인_리뷰_수정_불가() {
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(OWNER_ID, OWNER_ROLE, reviewId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_FORBIDDEN));
    }

    @Test
    void updateReview_별점_변경_시_store_통계_갱신됨() {
        UUID reviewId = UUID.randomUUID();
        store.addReview(4); // count=1, avg=4.0

        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);
        given(request.getRating()).willReturn(2);
        given(request.getContent()).willReturn("별로에요");

        ReviewUpdateResponse expectedResponse = ReviewUpdateResponse.builder()
                .reviewId(reviewId).rating(2).content("별로에요").build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toUpdateResponse(review)).willReturn(expectedResponse);

        reviewService.updateReview(CUSTOMER_ID, CUSTOMER_ROLE, reviewId, request);

        // 4 → 2 변경: avg = 4.0 + (2 - 4) / 1 = 2.0
        assertThat(store.getReviewCount()).isEqualTo(1);
        assertThat(store.getAverageRating()).isEqualTo(2.0);
        verify(storeRepository, times(1)).save(store);
    }

    @Test
    void updateReview_별점_동일하면_store_저장_생략() {
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);
        given(request.getRating()).willReturn(4); // oldRating == newRating
        given(request.getContent()).willReturn("내용만 수정");

        ReviewUpdateResponse expectedResponse = ReviewUpdateResponse.builder()
                .reviewId(reviewId).rating(4).content("내용만 수정").build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toUpdateResponse(review)).willReturn(expectedResponse);

        reviewService.updateReview(CUSTOMER_ID, CUSTOMER_ROLE, reviewId, request);

        verify(storeRepository, never()).save(any());
    }

    @Test
    void updateReview_실패_존재하지_않는_리뷰() {
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview(CUSTOMER_ID, CUSTOMER_ROLE, reviewId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
    }

    // ─────────────────────────────────────────────────────────────────
    // deleteReview
    // ─────────────────────────────────────────────────────────────────

    @Test
    void deleteReview_성공() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        reviewService.deleteReview(reviewId, CUSTOMER_ID, CUSTOMER_ROLE);

        assertThat(review.getDeletedAt()).isNotNull();
        assertThat(review.getDeletedBy()).isEqualTo(CUSTOMER_ID);
        verify(reviewRepository, times(1)).save(review);
    }

    @Test
    void deleteReview_실패_작성자_불일치() {
        UUID reviewId = UUID.randomUUID();
        // review 작성자 userId=1L, OTHER_ID=2L
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, OTHER_ID, OTHER_ROLE))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_FORBIDDEN));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void deleteReview_성공_MANAGER는_타인_리뷰_삭제_가능() {
        UUID reviewId = UUID.randomUUID();
        store.addReview(4);
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        reviewService.deleteReview(reviewId, MANAGER_ID, MANAGER_ROLE);

        assertThat(review.getDeletedAt()).isNotNull();
        assertThat(review.getDeletedBy()).isEqualTo(MANAGER_ID);
    }

    @Test
    void deleteReview_성공_MASTER는_타인_리뷰_삭제_가능() {
        UUID reviewId = UUID.randomUUID();
        store.addReview(4);
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        reviewService.deleteReview(reviewId, MASTER_ID, MASTER_ROLE);

        assertThat(review.getDeletedAt()).isNotNull();
        assertThat(review.getDeletedBy()).isEqualTo(MASTER_ID);
    }

    @Test
    void deleteReview_실패_OWNER는_타인_리뷰_삭제_불가() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, OWNER_ID, OWNER_ROLE))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_FORBIDDEN));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void deleteReview_이미_소프트삭제된_리뷰는_REVIEW_NOT_FOUND_반환() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, CUSTOMER_ID, CUSTOMER_ROLE))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_NOT_FOUND));

        verify(storeRepository, never()).save(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void deleteReview_성공_store_통계_갱신됨() {
        UUID reviewId = UUID.randomUUID();
        store.addReview(4); // count=1, avg=4.0
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        reviewService.deleteReview(reviewId, CUSTOMER_ID, CUSTOMER_ROLE);

        assertThat(store.getReviewCount()).isEqualTo(0);
        assertThat(store.getAverageRating()).isEqualTo(0.0);
        verify(storeRepository, times(1)).save(store);
    }

    @Test
    void deleteReview_실패_존재하지_않는_리뷰() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, CUSTOMER_ID, CUSTOMER_ROLE))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_NOT_FOUND));

        verify(reviewRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────
    // 트랜잭션 롤백 검증
    // ─────────────────────────────────────────────────────────────────

    @Test
    void createReview_트랜잭션_롤백_reviewRepository_save_예외시_store_통계_미반영() {
        ReviewCreateRequest request = new ReviewCreateRequest();

        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(user));
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(any())).willReturn(Optional.of(store));
        given(reviewMapper.toEntity(any(), any(), any())).willReturn(review);
        given(reviewRepository.save(any())).willThrow(new RuntimeException("DB 저장 오류"));

        assertThatThrownBy(() -> reviewService.createReview(CUSTOMER_ID, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB 저장 오류");

        verify(storeRepository, never()).save(store);
    }

    @Test
    void createReview_트랜잭션_롤백_storeRepository_save_예외시_예외_전파() {
        ReviewCreateRequest request = new ReviewCreateRequest();

        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(user));
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(any())).willReturn(Optional.of(store));
        given(reviewMapper.toEntity(any(), any(), any())).willReturn(review);
        given(reviewRepository.save(any())).willReturn(review);
        doThrow(new RuntimeException("store 저장 오류")).when(storeRepository).save(store);

        assertThatThrownBy(() -> reviewService.createReview(CUSTOMER_ID, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("store 저장 오류");
    }

    @Test
    void updateReview_트랜잭션_롤백_storeRepository_save_예외시_예외_전파() {
        UUID reviewId = UUID.randomUUID();
        store.addReview(4);

        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);
        given(request.getRating()).willReturn(2);
        given(request.getContent()).willReturn("별로에요");

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        doThrow(new RuntimeException("store 저장 오류")).when(storeRepository).save(any());

        assertThatThrownBy(() -> reviewService.updateReview(CUSTOMER_ID, CUSTOMER_ROLE, reviewId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("store 저장 오류");
    }

    @Test
    void deleteReview_트랜잭션_롤백_storeRepository_save_예외시_예외_전파() {
        UUID reviewId = UUID.randomUUID();
        store.addReview(4);

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        doThrow(new RuntimeException("store 저장 오류")).when(storeRepository).save(store);

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, CUSTOMER_ID, CUSTOMER_ROLE))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("store 저장 오류");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void deleteReview_트랜잭션_롤백_reviewRepository_save_예외시_예외_전파() {
        UUID reviewId = UUID.randomUUID();
        store.addReview(4);

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(storeRepository.save(store)).willReturn(store);
        doThrow(new RuntimeException("review 저장 오류")).when(reviewRepository).save(review);

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, CUSTOMER_ID, CUSTOMER_ROLE))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("review 저장 오류");
    }

    // ─────────────────────────────────────────────────────────────────
    // ErrorCode 존재 검증 (Order 연동 대기 항목 선검증)
    // ─────────────────────────────────────────────────────────────────

    @Test
    void REVIEW_ALREADY_EXISTS_에러코드가_정의되어_있음() {
        assertThat(ErrorCode.REVIEW_ALREADY_EXISTS).isNotNull();
        assertThat(ErrorCode.REVIEW_ALREADY_EXISTS.name()).isEqualTo("REVIEW_ALREADY_EXISTS");
    }

    @Test
    void createReview_중복검증_REVIEW_ALREADY_EXISTS_예외_발생_가능() {
        CustomException ex = new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS);
        assertThat(ex.getErrorCode().getStatus().value()).isEqualTo(409);
        assertThat(ex.getMessage()).isEqualTo("이미 리뷰를 작성했습니다.");
    }

    @Test
    void ORDER_NOT_FOUND_에러코드가_정의되어_있음() {
        assertThat(ErrorCode.ORDER_NOT_FOUND).isNotNull();
        assertThat(ErrorCode.ORDER_NOT_FOUND.name()).isEqualTo("ORDER_NOT_FOUND");
        assertThat(ErrorCode.ORDER_NOT_FOUND.getStatus().value()).isEqualTo(404);
    }
}

