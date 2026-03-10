package com.babjo.deliverycommerce.domain.review.repository;

import com.babjo.deliverycommerce.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    // @Where(deleted_at IS NULL) 자동 적용 - 삭제된 리뷰 자동 제외
    Optional<Review> findByReviewId(UUID reviewId);

    // 가게별 리뷰 목록 - @Where 자동 적용으로 삭제된 리뷰 제외
    List<Review> findAllByStore_StoreId(UUID storeId);

    // 가게별 리뷰 페이지 조회 - 페이지네이션 적용
    Page<Review> findAllByStore_StoreId(UUID storeId, Pageable pageable);

    // 유저별 리뷰 목록 - @Where 자동 적용으로 삭제된 리뷰 제외
    List<Review> findAllByUser_UserId(Long userId);

    // 유저별 리뷰 페이지 조회 - 페이지네이션 적용
    Page<Review> findAllByUser_UserId(Long userId, Pageable pageable);

    // 전체 리뷰 페이지 조회 (관리자용) - @Where 자동 적용
    @NonNull
    Page<Review> findAll(@NonNull Pageable pageable);

    // 관리자용 - 삭제된 리뷰 포함 단건 조회 (Native Query로 @Where 우회)
    @Query(value = "SELECT * FROM p_review WHERE review_id = :id", nativeQuery = true)
    Optional<Review> findByIdForAdmin(@Param("id") UUID id);

    // 관리자용 - 삭제된 리뷰 전체 목록
    @Query(value = "SELECT * FROM p_review WHERE deleted_at IS NOT NULL", nativeQuery = true)
    List<Review> findAllDeleted();

    // [TODO] Order 도메인 연결 후 주석 해제 - 중복 리뷰 방지용
    // boolean existsByOrder(Order order);
}
