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

    /**
     * 회원가입시 createdBy 후처리 목적
     * 초기에는 Auditing 할 id값이 존재하지 않아 save 이후 별도로 집어넣습니다.
     */
    public void initCreatedBy(Long userId) {
        this.createdBy = userId;
    }

    /**
     * 레코드 soft delete 처리는 해당 메서드를 사용하시면 됩니다
     * 삭제시간 -> 현재시간
     * 삭제자 -> principal에서 추출한 userId (로그인 유저 본인)
     */
    public void delete(Long deletedByUserId) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedByUserId;
    }

    /**
     * 삭제 여부 조회입니다.
     * 적용할 사용자 객체에 사용합니다.
     * true가 반환되면 '삭제된 상태'
     * false가 반환되면 '삭제되지 않은 상태 (정상 유저)' 입니다.
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
