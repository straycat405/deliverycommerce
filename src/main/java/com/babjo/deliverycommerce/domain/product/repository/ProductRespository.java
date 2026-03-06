package com.babjo.deliverycommerce.domain.product.repository;

import com.babjo.deliverycommerce.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRespository extends JpaRepository<Product, UUID> {

    // 단건 조회(삭제 제외)
    Optional<Product> findByProductIdAndStore_StoreIdAndDeletedAtIsNull(UUID storeId, UUID productId);

    // 가게별 전체 조회(삭제 제외)
    List<Product> findAllByStore_StoreIdAndDeletedAtIsNull(UUID storeId);

    // 가게별 전체 조회(숨김 제외, 고객용)
    List<Product> findAllByStore_StoreIdAndProductHideFalseAndDeletedAtIsNull(UUID storeId);

    // 카테고리 필터(삭제 제외)
    List<Product> findAllByStore_StoreIdAndProductCategoryAndDeletedAtIsNull(UUID StoreId, String category);

    // 카테고리 필터(숨김 제외, 고객용)
    List<Product> findAllByStore_StoreIdAndProductCategoryAndDeletedAtIsNullAndProductHideFalse(UUID StoreId, String category);
}

