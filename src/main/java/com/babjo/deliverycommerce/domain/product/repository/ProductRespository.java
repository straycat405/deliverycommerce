package com.babjo.deliverycommerce.domain.product.repository;

import com.babjo.deliverycommerce.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRespository extends JpaRepository<Product, UUID> {

    // 단건 조회(삭제 제외)
    Optional<Product> findByProductIdAndStore_StoreIdAndDeletedAtIsNull(UUID storeId, UUID productId);

    // 가게별 전체 조회(삭제 제외)
    Page<Product> findAllByStore_StoreIdAndDeletedAtIsNull(UUID storeId, Pageable pageable);

    // 가게별 전체 조회(숨김 제외, 고객용)
    Page<Product> findAllByStore_StoreIdAndProductHideFalseAndDeletedAtIsNull(UUID storeId, Pageable pageable);

    // 카테고리 필터(삭제 제외)
    Page<Product> findAllByStore_StoreIdAndProductCategoryAndDeletedAtIsNull(UUID StoreId, String category, Pageable pageable);

    // 카테고리 필터(숨김 제외, 고객용)
    Page<Product> findAllByStore_StoreIdAndProductCategoryAndDeletedAtIsNullAndProductHideFalse(UUID StoreId, String category, Pageable pageable);
}

