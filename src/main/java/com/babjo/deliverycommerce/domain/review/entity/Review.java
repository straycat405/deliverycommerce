package com.babjo.deliverycommerce.domain.review.entity;

import com.babjo.deliverycommerce.domain.store.entity.Store;
import com.babjo.deliverycommerce.global.common.entity.BaseEntity;
import com.babjo.deliverycommerce.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_review")
@Where(clause = "deleted_at IS NULL")
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // [TODO] Order 도메인 연결 후 주석 해제
//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "order_id", nullable = false, unique = true)
//    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String content;

    public static Review create(Store store, Integer rating, String content) {
        Review review = new Review();
        review.store = store;
        review.rating = rating;
        review.content = content;
        return review;
    }

    public void updateReview(Integer rating, String content) {
        this.rating = rating;
        this.content = content;
    }
}
