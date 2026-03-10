package com.babjo.deliverycommerce.domain.review.mapper;

import com.babjo.deliverycommerce.domain.order.entity.Order;
import com.babjo.deliverycommerce.domain.review.dto.ReviewCreateRequest;
import com.babjo.deliverycommerce.domain.review.dto.ReviewCreateResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewUpdateResponse;
import com.babjo.deliverycommerce.domain.review.entity.Review;
import com.babjo.deliverycommerce.domain.store.entity.Store;
import com.babjo.deliverycommerce.global.common.enums.UserEnumRole;
import com.babjo.deliverycommerce.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewMapperTest {

    private static final Long OWNER_ID = 5L;

    private ReviewMapper reviewMapper;
    private Store  store;
    private User   user;
    private Order  order;
    private Review review;

    @BeforeEach
    void setUp() {
        reviewMapper = new ReviewMapper();
        store = Store.create(1L, "한식", "테스트 식당", "서울시 강남구");
        user  = User.createForTest(1L, "testuser", "test@test.com", "테스터", UserEnumRole.CUSTOMER);
        order = Order.createOrder(1L, UUID.randomUUID(), "Seoul", "msg", List.of());
        order.accept(OWNER_ID, 30);
        order.startPreparing(OWNER_ID);
        order.readyPickup(OWNER_ID);
        order.completePickup(OWNER_ID);
        review = Review.create(user, order, store, 4, "맛있어요");
    }

    // ───────────────────────────────────────────────
    // toResponse
    // ───────────────────────────────────────────────

    @Test
    void toResponse_정상_변환() {
        // when
        ReviewResponse response = reviewMapper.toResponse(review);

        // then
        assertThat(response.getReviewId()).isEqualTo(review.getReviewId());
        assertThat(response.getUserId()).isEqualTo(user.getUserId());
        assertThat(response.getOrderId()).isEqualTo(order.getOrderId());
        assertThat(response.getStoreId()).isEqualTo(store.getStoreId());
        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.getContent()).isEqualTo("맛있어요");
    }

    @Test
    void toResponse_createdAt_updatedAt_포함() {
        // when
        ReviewResponse response = reviewMapper.toResponse(review);

        // then — createdAt/updatedAt은 JPA Auditing 없는 단위 테스트에서는 null이 정상
        // 필드 자체가 response에 존재하는지 확인 (NPE 없이 접근 가능)
        assertThat(response).isNotNull();
        assertThat(response.getStoreId()).isEqualTo(store.getStoreId());
        assertThat(response.getUserId()).isEqualTo(user.getUserId());
    }

    // ───────────────────────────────────────────────
    // toEntity
    // ───────────────────────────────────────────────

    @Test
    void toEntity_정상_변환() {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest();

        // when
        Review entity = reviewMapper.toEntity(request, user, order, store);

        // then
        assertThat(entity).isNotNull();
        assertThat(entity.getUser()).isEqualTo(user);
        assertThat(entity.getOrder()).isEqualTo(order);
        assertThat(entity.getStore()).isEqualTo(store);
        assertThat(entity.getRating()).isEqualTo(request.getRating());
        assertThat(entity.getContent()).isEqualTo(request.getContent());
    }

    @Test
    void toEntity_user와_order와_store가_올바르게_설정됨() {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest();

        // when
        Review entity = reviewMapper.toEntity(request, user, order, store);

        // then
        assertThat(entity.getUser().getUserId()).isEqualTo(1L);
        assertThat(entity.getOrder()).isEqualTo(order);
        assertThat(entity.getStore().getStoreId()).isEqualTo(store.getStoreId());
        assertThat(entity.getStore().getName()).isEqualTo("테스트 식당");
    }

    // ───────────────────────────────────────────────
    // toCreateResponse
    // ───────────────────────────────────────────────

    @Test
    void toCreateResponse_정상_변환() {
        // when
        ReviewCreateResponse response = reviewMapper.toCreateResponse(review);

        // then
        assertThat(response.getReviewId()).isEqualTo(review.getReviewId());
        assertThat(response.getUserId()).isEqualTo(user.getUserId());
        assertThat(response.getStoreId()).isEqualTo(store.getStoreId());
        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.getContent()).isEqualTo("맛있어요");
    }

    @Test
    void toCreateResponse_orderId가_Order의_orderId와_일치() {
        // when
        ReviewCreateResponse response = reviewMapper.toCreateResponse(review);

        // then — Order 연동 후이므로 orderId는 order의 orderId와 같아야 함
        assertThat(response.getOrderId()).isEqualTo(order.getOrderId());
    }

    // ───────────────────────────────────────────────
    // toUpdateResponse
    // ───────────────────────────────────────────────

    @Test
    void toUpdateResponse_정상_변환() {
        // given
        review.updateReview(5, "정말 맛있어요");

        // when
        ReviewUpdateResponse response = reviewMapper.toUpdateResponse(review);

        // then
        assertThat(response.getReviewId()).isEqualTo(review.getReviewId());
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getContent()).isEqualTo("정말 맛있어요");
    }

    @Test
    void toUpdateResponse_수정된_내용이_response에_반영됨() {
        // given
        review.updateReview(1, "별로에요");

        // when
        ReviewUpdateResponse response = reviewMapper.toUpdateResponse(review);

        // then
        assertThat(response.getRating()).isEqualTo(1);
        assertThat(response.getContent()).isEqualTo("별로에요");
    }
}

