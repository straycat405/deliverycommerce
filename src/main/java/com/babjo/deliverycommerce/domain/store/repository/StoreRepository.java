package com.babjo.deliverycommerce.domain.store.repository;

import com.babjo.deliverycommerce.domain.store.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {

    //단건 조회
    Optional<Store> findByStoreIdAndDeletedAtIsNull(UUID storeId);

    // 목록 조회
    Page<Store> findByDeletedAtIsNull(Pageable pageable);

    // 목록 조회 + category 필터
    Page<Store> findByDeletedAtIsNullAndCategory(String category, Pageable pageable);

    // 목록 조회 + name
    Page<Store> findByDeletedAtIsNullAndNameContaining(String name, Pageable pageable);

    // 목록 조회 + category + name
    Page<Store> findByDeletedAtIsNullAndCategoryAndNameContaining(String category, String name, Pageable pageable);

    Page<Store> findByDeletedAtIsNullAndCategoryAndNameContainingIgnoreCase(String category, String name, Pageable pageable);
}
