package com.babjo.deliverycommerce.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "p_review")
public class Review  extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID reviewId;

//    @ManyToOne
//    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = true)
//    private Long userId;
//
//    @ManyToOne
//    @JoinColumn(name = "order_id", referencedColumnName = "id", nullable = true)
//    private UUID orderId;
//
//    @ManyToOne
//    @JoinColumn(name = "store_id", referencedColumnName = "id", nullable = true)
//    private UUID storeId;

    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;
}
