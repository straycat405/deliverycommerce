package com.babjo.deliverycommerce.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@MappedSuperclass
public abstract class BaseEntity {


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

    /*삭제여부 확인*/
    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markCreatedBy(Long userId) {
        this.createdBy = userId;
    }

    public void markUpdatedBy(Long userId) {
        this.updatedBy = userId;
    }

    public void softDelete(Long actorUserId) {
        if (this.deletedAt != null) {
            return;
        }

        this.deletedAt = Instant.now();
        this.deletedBy = actorUserId;
    }
}
