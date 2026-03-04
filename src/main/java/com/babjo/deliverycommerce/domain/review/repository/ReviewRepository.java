package com.babjo.deliverycommerce.domain.review.repository;

import com.babjo.deliverycommerce.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByReviewIdAndDeletedAtIsNull(UUID reviewId);

    List<Review> findAllByDeletedAtIsNull();

    List<Review> findAllByStore_StoreIdAndDeletedAtIsNull(UUID storeId);

    // [TODO] Order 도메인 연결 후 주석 해제 - 중복 리뷰 방지용
    // boolean existsByOrderAndDeletedAtIsNull(Order order);
}
