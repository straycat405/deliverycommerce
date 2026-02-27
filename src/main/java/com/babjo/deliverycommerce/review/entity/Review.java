package com.babjo.deliverycommerce.review.entity;

import com.babjo.deliverycommerce.global.common.entity.BaseEntity;
import com.babjo.deliverycommerce.user.entity.User;
import jakarta.persistence.*;

import java.util.UUID;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "p_review")
public class Review  extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID reviewId;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;
//
//    @OneToOne
//    @JoinColumn(name = "order_id", referencedColumnName = "id", nullable = false, unique = true)
//    private Order order;
//
//    @ManyToOne
//    @JoinColumn(name = "store_id", referencedColumnName = "id", nullable = false)
//    private Store store;

    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;
}
