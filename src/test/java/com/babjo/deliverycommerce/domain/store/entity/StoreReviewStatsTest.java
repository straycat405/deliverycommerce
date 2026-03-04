package com.babjo.deliverycommerce.domain.store.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StoreReviewStatsTest {

    private static final double DELTA = 0.0001; // 부동소수점 허용 오차

    private Store store;

    @BeforeEach
    void setUp() {
        store = Store.create(1L, "한식", "테스트 식당", "서울시 강남구");
        // 초기 상태 검증
        assertThat(store.getReviewCount()).isEqualTo(0);
        assertThat(store.getAverageRating()).isEqualTo(0.0);
    }

    // ───────────────────────────────────────────────
    // addReview
    // ───────────────────────────────────────────────

    @Test
    void addReview_첫번째_리뷰_추가() {
        store.addReview(5);

        assertThat(store.getReviewCount()).isEqualTo(1);
        assertThat(store.getAverageRating()).isCloseTo(5.0, within(DELTA));
    }

    @Test
    void addReview_두번째_리뷰_추가_평균_계산() {
        store.addReview(4);
        store.addReview(2);

        assertThat(store.getReviewCount()).isEqualTo(2);
        assertThat(store.getAverageRating()).isCloseTo(3.0, within(DELTA));
    }

    @Test
    void addReview_세건_누적_평균_계산() {
        store.addReview(5);
        store.addReview(3);
        store.addReview(4);

        assertThat(store.getReviewCount()).isEqualTo(3);
        assertThat(store.getAverageRating()).isCloseTo(4.0, within(DELTA)); // (5+3+4)/3
    }

    @Test
    void addReview_최솟값_1점_추가() {
        store.addReview(1);

        assertThat(store.getReviewCount()).isEqualTo(1);
        assertThat(store.getAverageRating()).isCloseTo(1.0, within(DELTA));
    }

    @Test
    void addReview_최댓값_5점_추가() {
        store.addReview(5);

        assertThat(store.getReviewCount()).isEqualTo(1);
        assertThat(store.getAverageRating()).isCloseTo(5.0, within(DELTA));
    }

    @Test
    void addReview_여러건_누적_소수점_평균() {
        store.addReview(4);
        store.addReview(5);
        store.addReview(3);
        store.addReview(5);

        assertThat(store.getReviewCount()).isEqualTo(4);
        assertThat(store.getAverageRating()).isCloseTo(4.25, within(DELTA)); // (4+5+3+5)/4
    }

    // ───────────────────────────────────────────────
    // removeReview
    // ───────────────────────────────────────────────

    @Test
    void removeReview_리뷰_1개_삭제_후_0으로_초기화() {
        store.addReview(4);

        store.removeReview(4);

        assertThat(store.getReviewCount()).isEqualTo(0);
        assertThat(store.getAverageRating()).isCloseTo(0.0, within(DELTA));
    }

    @Test
    void removeReview_2개중_1개_삭제_후_평균_재계산() {
        store.addReview(4);
        store.addReview(2);

        store.removeReview(2); // 2점 리뷰 삭제

        assertThat(store.getReviewCount()).isEqualTo(1);
        assertThat(store.getAverageRating()).isCloseTo(4.0, within(DELTA));
    }

    @Test
    void removeReview_3개중_1개_삭제_후_평균_재계산() {
        store.addReview(5);
        store.addReview(3);
        store.addReview(4);
        // 현재 avg=4.0, count=3

        store.removeReview(3); // 3점 리뷰 삭제

        assertThat(store.getReviewCount()).isEqualTo(2);
        assertThat(store.getAverageRating()).isCloseTo(4.5, within(DELTA)); // (5+4)/2
    }

    @Test
    void removeReview_count가_0이하이면_0으로_초기화() {
        // count=0인 상태에서 removeReview 호출해도 안전해야 함
        store.removeReview(3);

        assertThat(store.getReviewCount()).isEqualTo(0);
        assertThat(store.getAverageRating()).isCloseTo(0.0, within(DELTA));
    }

    // ───────────────────────────────────────────────
    // updateReviewRating
    // ───────────────────────────────────────────────

    @Test
    void updateReviewRating_별점_변경_시_평균_재계산() {
        store.addReview(4);
        store.addReview(2);
        // avg=3.0, count=2

        store.updateReviewRating(2, 4); // 2점 → 4점으로 수정

        assertThat(store.getReviewCount()).isEqualTo(2); // count 변동 없음
        assertThat(store.getAverageRating()).isCloseTo(4.0, within(DELTA)); // (4+4)/2
    }

    @Test
    void updateReviewRating_동일_별점이면_평균_변동_없음() {
        store.addReview(4);
        store.addReview(4);
        // avg=4.0, count=2

        store.updateReviewRating(4, 4); // 4점 → 4점 (변경 없음)

        assertThat(store.getAverageRating()).isCloseTo(4.0, within(DELTA));
    }

    @Test
    void updateReviewRating_별점_낮아지면_평균_감소() {
        store.addReview(5);
        store.addReview(5);
        // avg=5.0, count=2

        store.updateReviewRating(5, 1); // 5점 → 1점으로 수정

        assertThat(store.getReviewCount()).isEqualTo(2);
        assertThat(store.getAverageRating()).isCloseTo(3.0, within(DELTA)); // (5+1)/2
    }

    @Test
    void updateReviewRating_count가_0이면_아무_변화없음() {
        // count=0인 상태에서 호출해도 안전해야 함
        store.updateReviewRating(3, 5);

        assertThat(store.getAverageRating()).isCloseTo(0.0, within(DELTA));
    }

    // ───────────────────────────────────────────────
    // 복합 시나리오
    // ───────────────────────────────────────────────

    @Test
    void 복합시나리오_추가_수정_삭제_순서() {
        // 1. 리뷰 3개 추가
        store.addReview(5);
        store.addReview(3);
        store.addReview(4);
        // avg=4.0, count=3

        // 2. 5점 리뷰를 2점으로 수정
        store.updateReviewRating(5, 2);
        // avg=(4*3 + (2-5))/3 = (12-3)/3 = 3.0, count=3
        assertThat(store.getReviewCount()).isEqualTo(3);
        assertThat(store.getAverageRating()).isCloseTo(3.0, within(DELTA));

        // 3. 3점 리뷰 삭제
        store.removeReview(3);
        // avg=(3*3 - 3)/(3-1) = 6/2 = 3.0, count=2
        assertThat(store.getReviewCount()).isEqualTo(2);
        assertThat(store.getAverageRating()).isCloseTo(3.0, within(DELTA));

        // 4. 새 리뷰 추가
        store.addReview(5);
        // avg=(3.0*2 + 5)/3 = 11/3 ≈ 3.666..., count=3
        assertThat(store.getReviewCount()).isEqualTo(3);
        assertThat(store.getAverageRating()).isCloseTo(11.0 / 3.0, within(DELTA));
    }
}

