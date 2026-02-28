package com.babjo.deliverycommerce.domain.store.repository;

import com.babjo.deliverycommerce.domain.store.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {

    // 단건 조회
    // - soft delete 되지 않은 가게만 조회
    Optional<Store> findByStoreIdAndDeletedAtIsNull(UUID storeId);

    // 목록 전체 조회
    // - 삭제되지 않은 가게만 페이지 단위로 조회
    Page<Store> findByDeletedAtIsNull(Pageable pageable);

    // 목록 조회 + category 필터
    // - category가 일치하는 가게만 조회
    Page<Store> findByDeletedAtIsNullAndCategory(String category, Pageable pageable);

    // 목록 조회 + name 검색
    // - 가게 이름에 검색어가 포함된 경우 조회
    // - 대소문자 구분 없이 검색
    Page<Store> findByDeletedAtIsNullAndNameContainingIgnoreCase(String name, Pageable pageable);

    // 목록 조회 + category + name 검색
    // - category 일치 + name 포함 조건을 동시에 만족하는 가게 조회
    // - 대소문자 구분 없이 검색
    Page<Store> findByDeletedAtIsNullAndCategoryAndNameContainingIgnoreCase(
            String category,
            String name,
            Pageable pageable
    );
}
