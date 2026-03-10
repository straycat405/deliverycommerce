package com.babjo.deliverycommerce.domain.review.service;

import com.babjo.deliverycommerce.domain.order.entity.Order;
import com.babjo.deliverycommerce.domain.order.entity.OrderStatus;
import com.babjo.deliverycommerce.domain.order.repository.OrderRepository;
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
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewService reviewService;

    private static final Long   CUSTOMER_ID   = 1L;
    private static final String CUSTOMER_ROLE = "ROLE_CUSTOMER";
    private static final Long   OTHER_ID      = 2L;
    private static final String OTHER_ROLE    = "ROLE_CUSTOMER";
    private static final Long   MANAGER_ID    = 3L;
    private static final String MANAGER_ROLE  = "ROLE_MANAGER";
    private static final Long   MASTER_ID     = 4L;
    private static final String MASTER_ROLE   = "ROLE_MASTER";
    private static final Long   OWNER_ID      = 5L;
    private static final String OWNER_ROLE    = "ROLE_OWNER";

    private Store  store;
    private User   user;
    private Order  order;
    private Review review;

    // ─── 헬퍼 ───────────────────────────────────────────────────────

    private ReviewSearchRequest defaultSearch() { return new ReviewSearchRequest(); }

    private ReviewSearchRequest searchByReviewId(UUID id) {
        ReviewSearchRequest s = new ReviewSearchRequest();
        s.setReviewId(id);
        return s;
    }

    private ReviewSearchRequest searchByStoreId(UUID id) {
        ReviewSearchRequest s = new ReviewSearchRequest();
        s.setStoreId(id);
        return s;
    }

    private ReviewCreateRequest createReviewRequest(UUID orderId, UUID storeId, Integer rating, String content) {
        try {
            ReviewCreateRequest req = new ReviewCreateRequest();
            setField(req, "orderId", orderId);
            setField(req, "storeId", storeId);
            setField(req, "rating",  rating);
            setField(req, "content", content);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @BeforeEach
    void setUp() {
        store  = Store.create(1L, "korean", "Test Store", "Seoul");
        user   = User.createForTest(1L, "testuser", "test@test.com", "tester", UserEnumRole.CUSTOMER);

        order = Order.createOrder(CUSTOMER_ID, UUID.randomUUID(), "Seoul", "no msg", List.of());
        order.accept(OWNER_ID, 30);
        order.startPreparing(OWNER_ID);
        order.readyPickup(OWNER_ID);
        order.completePickup(OWNER_ID);

        review = Review.create(user, order, store, 4, "good");
    }

    // ─── CREATE ─────────────────────────────────────────────────────

    @Test
    void createReview_success() {
        ReviewCreateRequest req = createReviewRequest(UUID.randomUUID(), UUID.randomUUID(), 4, "good");
        ReviewCreateResponse expected = ReviewCreateResponse.builder().rating(4).content("good").build();

        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(user));
        given(orderRepository.findById(any())).willReturn(Optional.of(order));
        given(reviewRepository.existsByOrder_OrderId(any())).willReturn(false);
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(any())).willReturn(Optional.of(store));
        given(reviewMapper.toEntity(any(), any(), any(), any())).willReturn(review);
        given(reviewRepository.save(any())).willReturn(review);
        given(reviewMapper.toCreateResponse(any())).willReturn(expected);

        ReviewCreateResponse result = reviewService.createReview(CUSTOMER_ID, req);
        assertThat(result.getRating()).isEqualTo(4);
        assertThat(result.getContent()).isEqualTo("good");
        verify(reviewRepository, times(1)).save(any());
    }

    @Test
    void createReview_store_stats_updated() {
        ReviewCreateRequest req = createReviewRequest(UUID.randomUUID(), UUID.randomUUID(), 4, "good");
        ReviewCreateResponse expected = ReviewCreateResponse.builder().rating(4).content("good").build();

        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(user));
        given(orderRepository.findById(any())).willReturn(Optional.of(order));
        given(reviewRepository.existsByOrder_OrderId(any())).willReturn(false);
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(any())).willReturn(Optional.of(store));
        given(reviewMapper.toEntity(any(), any(), any(), any())).willReturn(review);
        given(reviewRepository.save(any())).willReturn(review);
        given(reviewMapper.toCreateResponse(any())).willReturn(expected);

        reviewService.createReview(CUSTOMER_ID, req);

        assertThat(store.getReviewCount()).isEqualTo(1);
        assertThat(store.getAverageRating()).isEqualTo(4.0);
        verify(storeRepository, times(1)).save(store);
    }

    @Test
    void createReview_fail_user_not_found() {
        ReviewCreateRequest req = createReviewRequest(UUID.randomUUID(), UUID.randomUUID(), 4, "good");
        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(CUSTOMER_ID, req))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_fail_order_not_found() {
        ReviewCreateRequest req = createReviewRequest(UUID.randomUUID(), UUID.randomUUID(), 4, "good");
        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(user));
        given(orderRepository.findById(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(CUSTOMER_ID, req))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_fail_not_order_owner() {
        ReviewCreateRequest req = createReviewRequest(UUID.randomUUID(), UUID.randomUUID(), 4, "good");
        given(userRepository.findById(OTHER_ID)).willReturn(Optional.of(user));
        given(orderRepository.findById(any())).willReturn(Optional.of(order)); // order.userId = CUSTOMER_ID

        assertThatThrownBy(() -> reviewService.createReview(OTHER_ID, req))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_ORDER_USER));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_fail_order_not_completed_created_status() {
        Order incompleteOrder = Order.createOrder(CUSTOMER_ID, UUID.randomUUID(), "Seoul", "msg", List.of());
        ReviewCreateRequest req = createReviewRequest(UUID.randomUUID(), UUID.randomUUID(), 4, "good");

        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(user));
        given(orderRepository.findById(any())).willReturn(Optional.of(incompleteOrder));

        assertThatThrownBy(() -> reviewService.createReview(CUSTOMER_ID, req))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_COMPLETED));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_fail_order_not_completed_accepted_status() {
        Order acceptedOrder = Order.createOrder(CUSTOMER_ID, UUID.randomUUID(), "Seoul", "msg", List.of());
        acceptedOrder.accept(OWNER_ID, 30);
        ReviewCreateRequest req = createReviewRequest(UUID.randomUUID(), UUID.randomUUID(), 4, "good");

        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(user));
        given(orderRepository.findById(any())).willReturn(Optional.of(acceptedOrder));

        assertThatThrownBy(() -> reviewService.createReview(CUSTOMER_ID, req))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_COMPLETED));
    }

    @Test
    void createReview_fail_order_not_completed_preparing_status() {
        Order preparingOrder = Order.createOrder(CUSTOMER_ID, UUID.randomUUID(), "Seoul", "msg", List.of());
        preparingOrder.accept(OWNER_ID, 30);
        preparingOrder.startPreparing(OWNER_ID);
        ReviewCreateRequest req = createReviewRequest(UUID.randomUUID(), UUID.randomUUID(), 4, "good");

        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(user));
        given(orderRepository.findById(any())).willReturn(Optional.of(preparingOrder));

        assertThatThrownBy(() -> reviewService.createReview(CUSTOMER_ID, req))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_COMPLETED));
    }

    @Test
    void createReview_fail_order_canceled_status() {
        Order canceledOrder = Order.createOrder(CUSTOMER_ID, UUID.randomUUID(), "Seoul", "msg", List.of());
        canceledOrder.cancel(CUSTOMER_ID, "change mind");
        ReviewCreateRequest req = createReviewRequest(UUID.randomUUID(), UUID.randomUUID(), 4, "good");

        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(user));
        given(orderRepository.findById(any())).willReturn(Optional.of(canceledOrder));

        assertThatThrownBy(() -> reviewService.createReview(CUSTOMER_ID, req))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_COMPLETED));
    }

    @Test
    void createReview_fail_duplicate_review() {
        ReviewCreateRequest req = createReviewRequest(UUID.randomUUID(), UUID.randomUUID(), 4, "good");

        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(user));
        given(orderRepository.findById(any())).willReturn(Optional.of(order));
        given(reviewRepository.existsByOrder_OrderId(any())).willReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(CUSTOMER_ID, req))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_fail_store_not_found() {
        ReviewCreateRequest req = createReviewRequest(UUID.randomUUID(), UUID.randomUUID(), 4, "good");

        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(user));
        given(orderRepository.findById(any())).willReturn(Optional.of(order));
        given(reviewRepository.existsByOrder_OrderId(any())).willReturn(false);
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(CUSTOMER_ID, req))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.STORE_NOT_FOUND));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_fail_soft_deleted_store() {
        ReviewCreateRequest req = createReviewRequest(UUID.randomUUID(), UUID.randomUUID(), 4, "good");

        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(user));
        given(orderRepository.findById(any())).willReturn(Optional.of(order));
        given(reviewRepository.existsByOrder_OrderId(any())).willReturn(false);
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(CUSTOMER_ID, req))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.STORE_NOT_FOUND));
        verify(storeRepository, never()).save(any());
        verify(reviewRepository, never()).save(any());
    }

    // ─── READ - single by reviewId ───────────────────────────────────

    @Test
    void getReviews_customer_own_review_by_reviewId() {
        UUID reviewId = UUID.randomUUID();
        ReviewResponse expected = ReviewResponse.builder()
                .reviewId(reviewId).userId(CUSTOMER_ID).rating(4).content("good").build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toResponse(review)).willReturn(expected);

        Page<ReviewResponse> result = reviewService.getReviews(CUSTOMER_ID, CUSTOMER_ROLE, searchByReviewId(reviewId));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getReviewId()).isEqualTo(reviewId);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void getReviews_customer_others_review_forbidden() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.getReviews(OTHER_ID, OTHER_ROLE, searchByReviewId(reviewId)))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.REVIEW_FORBIDDEN));
    }

    @Test
    void getReviews_manager_can_read_others_review() {
        UUID reviewId = UUID.randomUUID();
        ReviewResponse expected = ReviewResponse.builder()
                .reviewId(reviewId).userId(CUSTOMER_ID).rating(4).content("good").build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toResponse(review)).willReturn(expected);

        Page<ReviewResponse> result = reviewService.getReviews(MANAGER_ID, MANAGER_ROLE, searchByReviewId(reviewId));
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getReviews_master_can_read_others_review() {
        UUID reviewId = UUID.randomUUID();
        ReviewResponse expected = ReviewResponse.builder()
                .reviewId(reviewId).userId(CUSTOMER_ID).rating(4).content("good").build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toResponse(review)).willReturn(expected);

        Page<ReviewResponse> result = reviewService.getReviews(MASTER_ID, MASTER_ROLE, searchByReviewId(reviewId));
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getReviews_reviewId_not_found() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviews(CUSTOMER_ID, CUSTOMER_ROLE, searchByReviewId(reviewId)))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
    }

    // ─── READ - list by storeId ──────────────────────────────────────

    @Test
    void getReviews_by_storeId_success() {
        UUID storeId = UUID.randomUUID();
        ReviewResponse expected = ReviewResponse.builder()
                .storeId(storeId).userId(CUSTOMER_ID).rating(3).content("ok").build();
        Page<Review> page = new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1);

        given(reviewRepository.findAllByStore_StoreId(eq(storeId), any(Pageable.class))).willReturn(page);
        given(reviewMapper.toResponse(review)).willReturn(expected);

        Page<ReviewResponse> result = reviewService.getReviews(CUSTOMER_ID, CUSTOMER_ROLE, searchByStoreId(storeId));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStoreId()).isEqualTo(storeId);
    }

    @Test
    void getReviews_by_storeId_empty() {
        UUID storeId = UUID.randomUUID();
        Page<Review> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        given(reviewRepository.findAllByStore_StoreId(eq(storeId), any(Pageable.class))).willReturn(emptyPage);

        Page<ReviewResponse> result = reviewService.getReviews(CUSTOMER_ID, CUSTOMER_ROLE, searchByStoreId(storeId));
        assertThat(result.getContent()).isEmpty();
    }

    // ─── READ - no params (role-based) ───────────────────────────────

    @Test
    void getReviews_customer_no_params_returns_own() {
        ReviewResponse expected = ReviewResponse.builder().userId(CUSTOMER_ID).rating(4).content("good").build();
        Page<Review> page = new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1);

        given(reviewRepository.findAllByUser_UserId(eq(CUSTOMER_ID), any(Pageable.class))).willReturn(page);
        given(reviewMapper.toResponse(review)).willReturn(expected);

        Page<ReviewResponse> result = reviewService.getReviews(CUSTOMER_ID, CUSTOMER_ROLE, defaultSearch());
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(CUSTOMER_ID);
        verify(reviewRepository, times(1)).findAllByUser_UserId(eq(CUSTOMER_ID), any(Pageable.class));
        verify(reviewRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getReviews_customer_no_params_empty() {
        Page<Review> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        given(reviewRepository.findAllByUser_UserId(eq(CUSTOMER_ID), any(Pageable.class))).willReturn(emptyPage);

        Page<ReviewResponse> result = reviewService.getReviews(CUSTOMER_ID, CUSTOMER_ROLE, defaultSearch());
        assertThat(result.getContent()).isEmpty();
        verify(reviewRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getReviews_owner_no_params_returns_own() {
        Page<Review> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        given(reviewRepository.findAllByUser_UserId(eq(OWNER_ID), any(Pageable.class))).willReturn(emptyPage);

        Page<ReviewResponse> result = reviewService.getReviews(OWNER_ID, OWNER_ROLE, defaultSearch());
        assertThat(result.getContent()).isEmpty();
        verify(reviewRepository, times(1)).findAllByUser_UserId(eq(OWNER_ID), any(Pageable.class));
        verify(reviewRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getReviews_manager_no_params_returns_all() {
        ReviewResponse expected = ReviewResponse.builder().userId(CUSTOMER_ID).rating(4).content("good").build();
        Page<Review> page = new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1);

        given(reviewRepository.findAll(any(Pageable.class))).willReturn(page);
        given(reviewMapper.toResponse(review)).willReturn(expected);

        Page<ReviewResponse> result = reviewService.getReviews(MANAGER_ID, MANAGER_ROLE, defaultSearch());
        assertThat(result.getContent()).hasSize(1);
        verify(reviewRepository, times(1)).findAll(any(Pageable.class));
        verify(reviewRepository, never()).findAllByUser_UserId(any(), any(Pageable.class));
    }

    @Test
    void getReviews_master_no_params_returns_all() {
        Page<Review> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        given(reviewRepository.findAll(any(Pageable.class))).willReturn(emptyPage);

        Page<ReviewResponse> result = reviewService.getReviews(MASTER_ID, MASTER_ROLE, defaultSearch());
        assertThat(result.getContent()).isEmpty();
        verify(reviewRepository, times(1)).findAll(any(Pageable.class));
        verify(reviewRepository, never()).findAllByUser_UserId(any(), any(Pageable.class));
    }

    // ─── UPDATE ─────────────────────────────────────────────────────

    @Test
    void updateReview_success() {
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequest req = mock(ReviewUpdateRequest.class);
        given(req.getRating()).willReturn(5);
        given(req.getContent()).willReturn("very good");
        ReviewUpdateResponse expected = ReviewUpdateResponse.builder()
                .reviewId(reviewId).rating(5).content("very good").build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toUpdateResponse(review)).willReturn(expected);

        ReviewUpdateResponse result = reviewService.updateReview(CUSTOMER_ID, CUSTOMER_ROLE, reviewId, req);
        assertThat(result.getReviewId()).isEqualTo(reviewId);
        assertThat(result.getRating()).isEqualTo(5);
    }

    @Test
    void updateReview_fail_not_owner() {
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequest req = mock(ReviewUpdateRequest.class);
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(OTHER_ID, OTHER_ROLE, reviewId, req))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.REVIEW_FORBIDDEN));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void updateReview_manager_can_update_others() {
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequest req = mock(ReviewUpdateRequest.class);
        given(req.getRating()).willReturn(3);
        given(req.getContent()).willReturn("manager edit");
        ReviewUpdateResponse expected = ReviewUpdateResponse.builder()
                .reviewId(reviewId).rating(3).content("manager edit").build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toUpdateResponse(review)).willReturn(expected);

        ReviewUpdateResponse result = reviewService.updateReview(MANAGER_ID, MANAGER_ROLE, reviewId, req);
        assertThat(result.getRating()).isEqualTo(3);
    }

    @Test
    void updateReview_master_can_update_others() {
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequest req = mock(ReviewUpdateRequest.class);
        given(req.getRating()).willReturn(2);
        given(req.getContent()).willReturn("master edit");
        ReviewUpdateResponse expected = ReviewUpdateResponse.builder()
                .reviewId(reviewId).rating(2).content("master edit").build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toUpdateResponse(review)).willReturn(expected);

        ReviewUpdateResponse result = reviewService.updateReview(MASTER_ID, MASTER_ROLE, reviewId, req);
        assertThat(result.getRating()).isEqualTo(2);
    }

    @Test
    void updateReview_fail_owner_cannot_update_others() {
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequest req = mock(ReviewUpdateRequest.class);
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(OWNER_ID, OWNER_ROLE, reviewId, req))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.REVIEW_FORBIDDEN));
    }

    @Test
    void updateReview_rating_change_updates_store_stats() {
        UUID reviewId = UUID.randomUUID();
        store.addReview(4);
        ReviewUpdateRequest req = mock(ReviewUpdateRequest.class);
        given(req.getRating()).willReturn(2);
        given(req.getContent()).willReturn("bad");
        ReviewUpdateResponse expected = ReviewUpdateResponse.builder()
                .reviewId(reviewId).rating(2).content("bad").build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toUpdateResponse(review)).willReturn(expected);

        reviewService.updateReview(CUSTOMER_ID, CUSTOMER_ROLE, reviewId, req);
        assertThat(store.getReviewCount()).isEqualTo(1);
        assertThat(store.getAverageRating()).isEqualTo(2.0);
        verify(storeRepository, times(1)).save(store);
    }

    @Test
    void updateReview_same_rating_skips_store_save() {
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequest req = mock(ReviewUpdateRequest.class);
        given(req.getRating()).willReturn(4);
        given(req.getContent()).willReturn("content only");
        ReviewUpdateResponse expected = ReviewUpdateResponse.builder()
                .reviewId(reviewId).rating(4).content("content only").build();

        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(reviewMapper.toUpdateResponse(review)).willReturn(expected);

        reviewService.updateReview(CUSTOMER_ID, CUSTOMER_ROLE, reviewId, req);
        verify(storeRepository, never()).save(any());
    }

    @Test
    void updateReview_fail_review_not_found() {
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequest req = mock(ReviewUpdateRequest.class);
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview(CUSTOMER_ID, CUSTOMER_ROLE, reviewId, req))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
    }

    // ─── DELETE (Soft Delete) ────────────────────────────────────────

    @Test
    void deleteReview_success() {
        UUID reviewId = UUID.randomUUID();
        store.addReview(4);
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        reviewService.deleteReview(reviewId, CUSTOMER_ID, CUSTOMER_ROLE);

        assertThat(review.getDeletedAt()).isNotNull();
        assertThat(review.getDeletedBy()).isEqualTo(CUSTOMER_ID);
        verify(reviewRepository, times(1)).save(review);
    }

    @Test
    void deleteReview_fail_not_owner() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, OTHER_ID, OTHER_ROLE))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.REVIEW_FORBIDDEN));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void deleteReview_manager_can_delete_others() {
        UUID reviewId = UUID.randomUUID();
        store.addReview(4);
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        reviewService.deleteReview(reviewId, MANAGER_ID, MANAGER_ROLE);

        assertThat(review.getDeletedAt()).isNotNull();
        assertThat(review.getDeletedBy()).isEqualTo(MANAGER_ID);
    }

    @Test
    void deleteReview_master_can_delete_others() {
        UUID reviewId = UUID.randomUUID();
        store.addReview(4);
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        reviewService.deleteReview(reviewId, MASTER_ID, MASTER_ROLE);

        assertThat(review.getDeletedAt()).isNotNull();
        assertThat(review.getDeletedBy()).isEqualTo(MASTER_ID);
    }

    @Test
    void deleteReview_fail_owner_cannot_delete_others() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, OWNER_ID, OWNER_ROLE))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.REVIEW_FORBIDDEN));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void deleteReview_already_soft_deleted_returns_not_found() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, CUSTOMER_ID, CUSTOMER_ROLE))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
        verify(storeRepository, never()).save(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void deleteReview_store_stats_updated() {
        UUID reviewId = UUID.randomUUID();
        store.addReview(4);
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));

        reviewService.deleteReview(reviewId, CUSTOMER_ID, CUSTOMER_ROLE);

        assertThat(store.getReviewCount()).isEqualTo(0);
        assertThat(store.getAverageRating()).isEqualTo(0.0);
        verify(storeRepository, times(1)).save(store);
    }

    @Test
    void deleteReview_fail_review_not_found() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, CUSTOMER_ID, CUSTOMER_ROLE))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
        verify(reviewRepository, never()).save(any());
    }

    // ─── Transaction rollback ────────────────────────────────────────

    @Test
    void createReview_rollback_when_reviewRepository_save_throws() {
        ReviewCreateRequest req = createReviewRequest(UUID.randomUUID(), UUID.randomUUID(), 4, "good");

        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(user));
        given(orderRepository.findById(any())).willReturn(Optional.of(order));
        given(reviewRepository.existsByOrder_OrderId(any())).willReturn(false);
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(any())).willReturn(Optional.of(store));
        given(reviewMapper.toEntity(any(), any(), any(), any())).willReturn(review);
        given(reviewRepository.save(any())).willThrow(new RuntimeException("db error"));

        assertThatThrownBy(() -> reviewService.createReview(CUSTOMER_ID, req))
                .isInstanceOf(RuntimeException.class).hasMessage("db error");
        verify(storeRepository, never()).save(store);
    }

    @Test
    void createReview_rollback_when_storeRepository_save_throws() {
        ReviewCreateRequest req = createReviewRequest(UUID.randomUUID(), UUID.randomUUID(), 4, "good");

        given(userRepository.findById(CUSTOMER_ID)).willReturn(Optional.of(user));
        given(orderRepository.findById(any())).willReturn(Optional.of(order));
        given(reviewRepository.existsByOrder_OrderId(any())).willReturn(false);
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(any())).willReturn(Optional.of(store));
        given(reviewMapper.toEntity(any(), any(), any(), any())).willReturn(review);
        given(reviewRepository.save(any())).willReturn(review);
        doThrow(new RuntimeException("store error")).when(storeRepository).save(store);

        assertThatThrownBy(() -> reviewService.createReview(CUSTOMER_ID, req))
                .isInstanceOf(RuntimeException.class).hasMessage("store error");
    }

    @Test
    void updateReview_rollback_when_storeRepository_save_throws() {
        UUID reviewId = UUID.randomUUID();
        store.addReview(4);
        ReviewUpdateRequest req = mock(ReviewUpdateRequest.class);
        given(req.getRating()).willReturn(2);
        given(req.getContent()).willReturn("bad");
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        doThrow(new RuntimeException("store error")).when(storeRepository).save(any());

        assertThatThrownBy(() -> reviewService.updateReview(CUSTOMER_ID, CUSTOMER_ROLE, reviewId, req))
                .isInstanceOf(RuntimeException.class).hasMessage("store error");
    }

    @Test
    void deleteReview_rollback_when_storeRepository_save_throws() {
        UUID reviewId = UUID.randomUUID();
        store.addReview(4);
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        doThrow(new RuntimeException("store error")).when(storeRepository).save(store);

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, CUSTOMER_ID, CUSTOMER_ROLE))
                .isInstanceOf(RuntimeException.class).hasMessage("store error");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void deleteReview_rollback_when_reviewRepository_save_throws() {
        UUID reviewId = UUID.randomUUID();
        store.addReview(4);
        given(reviewRepository.findByReviewId(reviewId)).willReturn(Optional.of(review));
        given(storeRepository.save(store)).willReturn(store);
        doThrow(new RuntimeException("review error")).when(reviewRepository).save(review);

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, CUSTOMER_ID, CUSTOMER_ROLE))
                .isInstanceOf(RuntimeException.class).hasMessage("review error");
    }

    // ─── ErrorCode 검증 ──────────────────────────────────────────────

    @Test
    void errorCode_REVIEW_ALREADY_EXISTS_is_defined() {
        assertThat(ErrorCode.REVIEW_ALREADY_EXISTS).isNotNull();
        assertThat(ErrorCode.REVIEW_ALREADY_EXISTS.name()).isEqualTo("REVIEW_ALREADY_EXISTS");
    }

    @Test
    void errorCode_REVIEW_ALREADY_EXISTS_is_conflict_409() {
        CustomException ex = new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
        assertThat(ex.getErrorCode().getStatus().value()).isEqualTo(409);
    }

    @Test
    void errorCode_ORDER_NOT_FOUND_is_defined() {
        assertThat(ErrorCode.ORDER_NOT_FOUND).isNotNull();
        assertThat(ErrorCode.ORDER_NOT_FOUND.getStatus().value()).isEqualTo(404);
    }

    @Test
    void errorCode_ORDER_NOT_COMPLETED_is_defined() {
        assertThat(ErrorCode.ORDER_NOT_COMPLETED).isNotNull();
        assertThat(ErrorCode.ORDER_NOT_COMPLETED.getStatus().value()).isEqualTo(400);
    }

    @Test
    void errorCode_NOT_ORDER_USER_is_defined() {
        assertThat(ErrorCode.NOT_ORDER_USER).isNotNull();
        assertThat(ErrorCode.NOT_ORDER_USER.getStatus().value()).isEqualTo(403);
    }
}

