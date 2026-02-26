package com.babjo.deliverycommerce.domain.store.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "p_store")
public class Store {

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    public static Store create(Long ownerId, String category, String name, String address) {
        Store store = new Store();
        store.ownerId = ownerId;
        store.category = category;
        store.name = name;
        store.address = address;

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

        this.updatedBy = actorUserId;
    }

    public void softDelete(Long actorUserId) {
        if (this.deletedAt != null) {
            return;
        }

        this.deletedAt = Instant.now();
        this.deletedBy = actorUserId;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }


}