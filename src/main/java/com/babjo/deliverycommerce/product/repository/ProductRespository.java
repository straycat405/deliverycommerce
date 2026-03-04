package com.babjo.deliverycommerce.product.repository;

import com.babjo.deliverycommerce.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRespository extends JpaRepository<Product, UUID> {

    // Store 연결되면 findAllByStore_StoreIdAndDeletedAtIsNull() 형태로 확장

    // 단건 조회(삭제 제외)
    Optional<Product> findByProductIdAndDeletedAtIsNull(UUID productId);

    // 가게별 전체 조회(삭제 제외)
    List<Product> findAllByDeletedAtIsNull();

    // 카테고리 필터
    List<Product> findAllByProductCategoryAndDeletedAtIsNull(String category);
    List<Product> findAllByProductCategoryAndDeletedAtIsNullAndProductHideFalse(String category);

    // 숨김 제외(고객용)
    List<Product> findAllByProductHideFalseAndDeletedAtIsNull();
}

