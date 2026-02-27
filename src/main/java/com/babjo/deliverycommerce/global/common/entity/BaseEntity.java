package com.babjo.deliverycommerce.global.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass // 테이블로 생성되지 않고, 상속받은 엔티티에 필드값만 내려줌
@EntityListeners(AuditingEntityListener.class) // Auditing 자동 처리
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column
    private Long createdBy;         // 생성자 p_user.user_id

    @LastModifiedDate
    @Column
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column
    private Long updatedBy;         // 수정자 p_user.user_id

    @Column
    private LocalDateTime deletedAt;

    @Column
    private Long deletedBy;         // 삭제자 user_id

    // 회원가입시 createdBy 후처리 목적
    public void initCreatedBy(Long userId) {
        this.createdBy = userId;
    }

    public void delete(Long deletedByUserId) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedByUserId;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
