package com.babjo.deliverycommerce.domain.store.entity;

import com.babjo.deliverycommerce.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "p_store")
public class Store extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "store_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID storeId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "category", length = 50, nullable = false)
    private String category;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Column(name = "average_rating", nullable = false)
    private Double averageRating = 0.0;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;


    public static Store create(Long ownerId, String category, String name, String address) {
        Store store = new Store();
        store.ownerId = ownerId;
        store.category = category;
        store.name = name;
        store.address = address;

        store.markCreatedBy(ownerId);
        return store;
    }

    public void update(String category, String name, String address, Long actorUserId) {
        if (category != null) {
            this.category = category;
        }
        if (name != null) {
            this.name = name;
        }
        if (address != null) {
            this.address = address;
        }


        this.markUpdatedBy(actorUserId);
    }
}