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
    private UserPrincipal masterPrincipal;  // userId=4L, MASTER
    private UserPrincipal ownerPrincipal;   // userId=5L, OWNER (리뷰 작성 불가, 삭제/수정도 본인 리뷰 아님)
    private Store store;
    private User user;
    private Review review;

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal(1L, "testuser", "CUSTOMER");
        otherPrincipal = new UserPrincipal(2L, "otheruser", "CUSTOMER");
        managerPrincipal = new UserPrincipal(3L, "manager", "MANAGER");
        masterPrincipal = new UserPrincipal(4L, "master", "MASTER");
        ownerPrincipal = new UserPrincipal(5L, "owner", "OWNER");

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

        ReviewCreateResponse expectedResponse = ReviewCreateResponse.builder()
                .rating(4)
                .content("맛있어요")
                .build();

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

        ReviewCreateResponse expectedResponse = ReviewCreateResponse.builder()
                .rating(4)
                .content("맛있어요")
                .build();

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

    @Test
    void createReview_실패_소프트삭제된_가게() {
        // given — findByStoreIdAndDeletedAtIsNull가 empty를 반환 = soft-delete된 가게 포함
        ReviewCreateRequest request = new ReviewCreateRequest();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        // soft-delete된 가게는 findByStoreIdAndDeletedAtIsNull에서 empty 반환
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(principal, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.STORE_NOT_FOUND));

        verify(storeRepository, never()).save(any());
        verify(reviewRepository, never()).save(any());
    }

    // ───────────────────────────────────────────────
    // getReviews
    // ───────────────────────────────────────────────

    @Test
    void getReviews_CUSTOMER_본인_리뷰_단건조회_성공() {
        // given — review 작성자 userId=1L, principal userId=1L (본인)
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
        List<ReviewResponse> result = reviewService.getReviews(principal, reviewId, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReviewId()).isEqualTo(reviewId);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
        verify(reviewRepository, times(1)).findByReviewId(reviewId);
        verify(reviewRepository, never()).findAllByStore_StoreId(any());
        verify(reviewRepository, never()).findAll();
    }

    @Test
    void getReviews_CUSTOMER_타인_리뷰_단건조회_실패() {
        // given — review 작성자 userId=1L, otherPrincipal userId=2L (타인)
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.getReviews(otherPrincipal, reviewId, null))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_FORBIDDEN));
    }

    @Test
    void getReviews_MANAGER_타인_리뷰_단건조회_성공() {
        // given — MANAGER는 타인 리뷰도 조회 가능
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
        List<ReviewResponse> result = reviewService.getReviews(managerPrincipal, reviewId, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    void getReviews_단건조회_실패_존재하지_않는_리뷰() {
        // given
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.getReviews(principal, reviewId, null))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
    }

    @Test
    void getReviews_MASTER_타인_리뷰_단건조회_성공() {
        // given — MASTER도 타인 리뷰 조회 가능
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
        List<ReviewResponse> result = reviewService.getReviews(masterPrincipal, reviewId, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    void getReviews_storeId_필터_목록조회_성공_CUSTOMER() {
        // given — storeId 조회는 CUSTOMER도 모든 리뷰 조회 가능 (공개 정보)
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
        List<ReviewResponse> result = reviewService.getReviews(principal, null, storeId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStoreId()).isEqualTo(storeId);
        verify(reviewRepository, times(1)).findAllByStore_StoreId(storeId);
        verify(reviewRepository, never()).findAll();
        verify(reviewRepository, never()).findAllByUser_UserId(any());
    }

    @Test
    void getReviews_storeId_필터_결과없음_빈목록_반환() {
        // given
        UUID storeId = UUID.randomUUID();
        given(reviewRepository.findAllByStore_StoreId(storeId)).willReturn(List.of());

        // when
        List<ReviewResponse> result = reviewService.getReviews(principal, null, storeId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void getReviews_파라미터없음_CUSTOMER_본인_리뷰만_반환() {
        // given — CUSTOMER는 본인 작성 리뷰만 반환
        ReviewResponse reviewResponse = ReviewResponse.builder()
                .userId(1L)
                .rating(4)
                .content("맛있어요")
                .build();

        given(reviewRepository.findAllByUser_UserId(1L)).willReturn(List.of(review));
        given(reviewMapper.toResponse(review)).willReturn(reviewResponse);

        // when
        List<ReviewResponse> result = reviewService.getReviews(principal, null, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
        verify(reviewRepository, times(1)).findAllByUser_UserId(1L);
        verify(reviewRepository, never()).findAll();
        verify(reviewRepository, never()).findByReviewId(any());
        verify(reviewRepository, never()).findAllByStore_StoreId(any());
    }

    @Test
    void getReviews_파라미터없음_CUSTOMER_본인_리뷰_없으면_빈목록() {
        // given
        given(reviewRepository.findAllByUser_UserId(1L)).willReturn(List.of());

        // when
        List<ReviewResponse> result = reviewService.getReviews(principal, null, null);

        // then
        assertThat(result).isEmpty();
        verify(reviewRepository, times(1)).findAllByUser_UserId(1L);
        verify(reviewRepository, never()).findAll();
    }

    @Test
    void getReviews_파라미터없음_OWNER_본인_리뷰만_반환() {
        // given — OWNER도 CUSTOMER와 동일하게 본인 리뷰만 반환
        given(reviewRepository.findAllByUser_UserId(5L)).willReturn(List.of());

        // when
        List<ReviewResponse> result = reviewService.getReviews(ownerPrincipal, null, null);

        // then
        assertThat(result).isEmpty();
        verify(reviewRepository, times(1)).findAllByUser_UserId(5L);
        verify(reviewRepository, never()).findAll();
    }

    @Test
    void getReviews_파라미터없음_MANAGER_전체_리뷰_반환() {
        // given — MANAGER는 전체 리뷰 조회 가능
        ReviewResponse reviewResponse = ReviewResponse.builder()
                .userId(1L)
                .rating(4)
                .content("맛있어요")
                .build();

        given(reviewRepository.findAll()).willReturn(List.of(review));
        given(reviewMapper.toResponse(review)).willReturn(reviewResponse);

        // when
        List<ReviewResponse> result = reviewService.getReviews(managerPrincipal, null, null);

        // then
        assertThat(result).hasSize(1);
        verify(reviewRepository, times(1)).findAll();
        verify(reviewRepository, never()).findAllByUser_UserId(any());
    }

    @Test
    void getReviews_파라미터없음_MASTER_전체_리뷰_반환() {
        // given — MASTER도 전체 리뷰 조회 가능
        given(reviewRepository.findAll()).willReturn(List.of());

        // when
        List<ReviewResponse> result = reviewService.getReviews(masterPrincipal, null, null);

        // then
        assertThat(result).isEmpty();
        verify(reviewRepository, times(1)).findAll();
        verify(reviewRepository, never()).findAllByUser_UserId(any());
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

        ReviewUpdateResponse expectedResponse = ReviewUpdateResponse.builder()
                .reviewId(reviewId)
                .rating(5)
                .content("정말 맛있어요")
                .build();

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

        ReviewUpdateResponse expectedResponse = ReviewUpdateResponse.builder()
                .reviewId(reviewId)
                .rating(3)
                .content("관리자 수정")
                .build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toUpdateResponse(review)).willReturn(expectedResponse);

        // when
        ReviewUpdateResponse result = reviewService.updateReview(managerPrincipal, reviewId, request);

        // then
        assertThat(result.getRating()).isEqualTo(3);
    }

    @Test
    void updateReview_성공_MASTER는_타인_리뷰_수정_가능() {
        // given
        UUID reviewId = UUID.randomUUID();

        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);
        given(request.getRating()).willReturn(2);
        given(request.getContent()).willReturn("마스터 수정");

        ReviewUpdateResponse expectedResponse = ReviewUpdateResponse.builder()
                .reviewId(reviewId)
                .rating(2)
                .content("마스터 수정")
                .build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toUpdateResponse(review)).willReturn(expectedResponse);

        // when
        ReviewUpdateResponse result = reviewService.updateReview(masterPrincipal, reviewId, request);

        // then
        assertThat(result.getRating()).isEqualTo(2);
    }

    @Test
    void updateReview_실패_OWNER는_타인_리뷰_수정_불가() {
        // given — OWNER는 본인 리뷰가 아닌 타인 리뷰 수정 불가
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(ownerPrincipal, reviewId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_FORBIDDEN));
    }

    @Test
    void updateReview_별점_변경_시_store_통계_갱신됨() {
        // given
        UUID reviewId = UUID.randomUUID();
        store.addReview(4); // count=1, avg=4.0

        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);
        given(request.getRating()).willReturn(2); // 4 → 2로 변경
        given(request.getContent()).willReturn("별로에요");

        ReviewUpdateResponse expectedResponse = ReviewUpdateResponse.builder()
                .reviewId(reviewId)
                .rating(2)
                .content("별로에요")
                .build();

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

        ReviewUpdateResponse expectedResponse = ReviewUpdateResponse.builder()
                .reviewId(reviewId)
                .rating(4)
                .content("내용만 수정")
                .build();

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
    void deleteReview_성공_MASTER는_타인_리뷰_삭제_가능() {
        // given
        UUID reviewId = UUID.randomUUID();
        store.addReview(4);
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        // when
        reviewService.deleteReview(reviewId, masterPrincipal);

        // then
        assertThat(review.getDeletedAt()).isNotNull();
        assertThat(review.getDeletedBy()).isEqualTo(4L);
    }

    @Test
    void deleteReview_실패_OWNER는_타인_리뷰_삭제_불가() {
        // given — OWNER는 본인 리뷰가 아닌 타인 리뷰 삭제 불가
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, ownerPrincipal))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_FORBIDDEN));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void deleteReview_이미_소프트삭제된_리뷰는_REVIEW_NOT_FOUND_반환() {
        // given
        // @Where(deleted_at IS NULL) 적용으로 이미 삭제된 리뷰는
        // findByReviewId가 empty를 반환 → REVIEW_NOT_FOUND 발생이 올바른 동작
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, principal))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REVIEW_NOT_FOUND));

        verify(storeRepository, never()).save(any());
        verify(reviewRepository, never()).save(any());
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

    // ───────────────────────────────────────────────
    // 트랜잭션 롤백 검증
    // ───────────────────────────────────────────────

    @Test
    void createReview_트랜잭션_롤백_reviewRepository_save_예외시_store_통계_미반영() {
        // given — reviewRepository.save 시 RuntimeException 발생 → @Transactional 롤백
        ReviewCreateRequest request = new ReviewCreateRequest();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(any())).willReturn(Optional.of(store));
        given(reviewMapper.toEntity(any(), any(), any())).willReturn(review);
        given(reviewRepository.save(any())).willThrow(new RuntimeException("DB 저장 오류"));

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(principal, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB 저장 오류");

        // store.addReview는 reviewRepository.save 이후에 호출되므로 아직 호출 안 됨
        // storeRepository.save 도 호출되지 않아야 함
        verify(storeRepository, never()).save(store);
    }

    @Test
    void createReview_트랜잭션_롤백_storeRepository_save_예외시_예외_전파() {
        // given — storeRepository.save 시 RuntimeException 발생
        ReviewCreateRequest request = new ReviewCreateRequest();


        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(any())).willReturn(Optional.of(store));
        given(reviewMapper.toEntity(any(), any(), any())).willReturn(review);
        given(reviewRepository.save(any())).willReturn(review);
        // storeRepository.save 시 예외 발생
        doThrow(new RuntimeException("store 저장 오류")).when(storeRepository).save(store);

        // when & then — 예외가 전파되어야 함 (@Transactional이 전체 롤백)
        assertThatThrownBy(() -> reviewService.createReview(principal, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("store 저장 오류");
    }

    @Test
    void updateReview_트랜잭션_롤백_storeRepository_save_예외시_예외_전파() {
        // given — 별점 변경 + storeRepository.save 시 RuntimeException 발생
        UUID reviewId = UUID.randomUUID();
        store.addReview(4); // count=1, avg=4.0

        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);
        given(request.getRating()).willReturn(2); // 4 → 2 변경
        given(request.getContent()).willReturn("별로에요");

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        doThrow(new RuntimeException("store 저장 오류")).when(storeRepository).save(any());

        // when & then — 예외가 전파되어야 함
        assertThatThrownBy(() -> reviewService.updateReview(principal, reviewId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("store 저장 오류");
    }

    @Test
    void deleteReview_트랜잭션_롤백_storeRepository_save_예외시_예외_전파() {
        // given — store 통계 갱신 후 storeRepository.save 시 예외 발생
        UUID reviewId = UUID.randomUUID();
        store.addReview(4); // count=1, avg=4.0

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        doThrow(new RuntimeException("store 저장 오류")).when(storeRepository).save(store);

        // when & then — 예외 전파 → @Transactional 롤백 (review soft-delete 미반영)
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, principal))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("store 저장 오류");

        // reviewRepository.save 는 storeRepository.save 이후이므로 호출되지 않아야 함
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void deleteReview_트랜잭션_롤백_reviewRepository_save_예외시_예외_전파() {
        // given — storeRepository.save 성공 후 reviewRepository.save 시 예외 발생
        UUID reviewId = UUID.randomUUID();
        store.addReview(4); // count=1, avg=4.0

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        // storeRepository.save는 정상 동작
        given(storeRepository.save(store)).willReturn(store);
        // reviewRepository.save 시 예외 발생
        doThrow(new RuntimeException("review 저장 오류")).when(reviewRepository).save(review);

        // when & then — 예외 전파 → @Transactional이 전체 롤백
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, principal))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("review 저장 오류");
    }

    // ───────────────────────────────────────────────
    // 중복 데이터 검증 (REVIEW_ALREADY_EXISTS ErrorCode 존재 확인)
    // [TODO] Order 도메인 연결 후 실제 중복 방지 로직 테스트로 교체
    // ───────────────────────────────────────────────

    @Test
    void REVIEW_ALREADY_EXISTS_에러코드가_정의되어_있음() {
        // Order 도메인 연결 전에도 REVIEW_ALREADY_EXISTS ErrorCode가
        // 정의되어 있어 추후 연결 시 바로 사용 가능한지 검증
        assertThat(ErrorCode.REVIEW_ALREADY_EXISTS).isNotNull();
        assertThat(ErrorCode.REVIEW_ALREADY_EXISTS.name()).isEqualTo("REVIEW_ALREADY_EXISTS");
    }

    @Test
    void createReview_중복검증_REVIEW_ALREADY_EXISTS_예외_발생_가능() {
        // Order 도메인 연결 후 중복 리뷰 시 REVIEW_ALREADY_EXISTS를 던져야 함을 검증
        // 현재는 서비스 레이어 외부에서 직접 예외를 생성하여 동작 확인
        CustomException ex = new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS);
        assertThat(ex.getErrorCode().getStatus().value()).isEqualTo(409);
        assertThat(ex.getMessage()).isEqualTo("이미 리뷰를 작성했습니다.");
    }

    // ───────────────────────────────────────────────
    // 예외 상황 — 주문 미완료 (Order 도메인 제외, ErrorCode 레벨 검증)
    // ───────────────────────────────────────────────

    @Test
    void ORDER_NOT_FOUND_에러코드가_정의되어_있음() {
        // Order 도메인 연결 전에도 ORDER_NOT_FOUND ErrorCode가 정의되어 있는지 검증
        assertThat(ErrorCode.ORDER_NOT_FOUND).isNotNull();
        assertThat(ErrorCode.ORDER_NOT_FOUND.name()).isEqualTo("ORDER_NOT_FOUND");
        assertThat(ErrorCode.ORDER_NOT_FOUND.getStatus().value()).isEqualTo(404);
    }

}


