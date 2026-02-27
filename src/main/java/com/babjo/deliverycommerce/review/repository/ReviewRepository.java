package com.babjo.deliverycommerce.review.repository;

import com.babjo.deliverycommerce.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByReviewIdAndDeletedAtIsNull(UUID reviewId);

    List<Review> findAllByDeletedAtIsNull();

    // [TODO] Store 도메인 연결 후 주석 해제
    // List<Review> findAllByStore_StoreIdAndDeletedAtIsNull(UUID storeId);
}
