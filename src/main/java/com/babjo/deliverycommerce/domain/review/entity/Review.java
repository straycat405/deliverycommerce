package com.babjo.deliverycommerce.domain.review.entity;

import com.babjo.deliverycommerce.domain.store.entity.Store;
import com.babjo.deliverycommerce.global.common.entity.BaseEntity;
import com.babjo.deliverycommerce.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "p_review")
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "userId", nullable = false)
    private User user;

    // [TODO] Order 도메인 연결 후 주석 해제
//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "order_id", referencedColumnName = "orderId", nullable = false, unique = true)
//    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", referencedColumnName = "storeId", nullable = false)
    private Store store;

    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String content;
}
