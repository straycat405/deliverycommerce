package com.babjo.deliverycommerce.domain.review.entity;

import com.babjo.deliverycommerce.domain.store.entity.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewTest {

    private Store store;

    @BeforeEach
    void setUp() {
        store = Store.create(1L, "한식", "테스트 식당", "서울시 강남구");
    }

    // ───────────────────────────────────────────────
    // create() 정적 팩토리 메서드
    // ───────────────────────────────────────────────

    @Test
    void create_정상_생성() {
        // when
        Review review = Review.create(store, 4, "맛있어요");

        // then
        assertThat(review.getStore()).isEqualTo(store);
        assertThat(review.getRating()).isEqualTo(4);
        assertThat(review.getContent()).isEqualTo("맛있어요");
    }

    @Test
    void create_rating_최솟값_1() {
        // when
        Review review = Review.create(store, 1, "별로에요");

        // then
        assertThat(review.getRating()).isEqualTo(1);
    }

    @Test
    void create_rating_최댓값_5() {
        // when
        Review review = Review.create(store, 5, "최고에요");

        // then
        assertThat(review.getRating()).isEqualTo(5);
    }

    @Test
    void create_초기에_삭제되지_않은_상태() {
        // when
        Review review = Review.create(store, 4, "맛있어요");

        // then
        assertThat(review.isDeleted()).isFalse();
        assertThat(review.getDeletedAt()).isNull();
        assertThat(review.getDeletedBy()).isNull();
    }

    // ───────────────────────────────────────────────
    // updateReview()
    // ───────────────────────────────────────────────

    @Test
    void updateReview_rating과_content_변경됨() {
        // given
        Review review = Review.create(store, 3, "보통이에요");

        // when
        review.updateReview(5, "다시 먹어보니 최고에요");

        // then
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getContent()).isEqualTo("다시 먹어보니 최고에요");
    }

    @Test
    void updateReview_rating만_변경해도_content는_새값으로_설정() {
        // given
        Review review = Review.create(store, 4, "맛있어요");

        // when
        review.updateReview(2, "맛있어요");

        // then
        assertThat(review.getRating()).isEqualTo(2);
        assertThat(review.getContent()).isEqualTo("맛있어요");
    }

    @Test
    void updateReview_여러번_호출해도_마지막_값으로_덮어씀() {
        // given
        Review review = Review.create(store, 3, "처음엔 보통");

        // when
        review.updateReview(4, "두번째 수정");
        review.updateReview(5, "세번째 수정");

        // then
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getContent()).isEqualTo("세번째 수정");
    }

    // ───────────────────────────────────────────────
    // delete() — BaseEntity 상속
    // ───────────────────────────────────────────────

    @Test
    void delete_삭제_처리후_deletedAt_설정됨() {
        // given
        Review review = Review.create(store, 4, "맛있어요");
        Long deletedByUserId = 1L;

        // when
        review.delete(deletedByUserId);

        // then
        assertThat(review.isDeleted()).isTrue();
        assertThat(review.getDeletedAt()).isNotNull();
        assertThat(review.getDeletedBy()).isEqualTo(deletedByUserId);
    }

    @Test
    void delete_이미_삭제된_리뷰는_deletedAt이_변경되지_않음() {
        // given
        Review review = Review.create(store, 4, "맛있어요");
        review.delete(1L);
        var firstDeletedAt = review.getDeletedAt();

        // when — 동일 리뷰에 다시 delete 호출
        review.delete(2L);

        // then — deletedAt, deletedBy 모두 최초값 유지 (idempotent)
        assertThat(review.getDeletedAt()).isEqualTo(firstDeletedAt);
        assertThat(review.getDeletedBy()).isEqualTo(1L); // 최초 삭제자 유지
    }

    @Test
    void isDeleted_삭제전_false() {
        // given
        Review review = Review.create(store, 4, "맛있어요");

        // then
        assertThat(review.isDeleted()).isFalse();
    }

    @Test
    void isDeleted_삭제후_true() {
        // given
        Review review = Review.create(store, 4, "맛있어요");

        // when
        review.delete(1L);

        // then
        assertThat(review.isDeleted()).isTrue();
    }
}

