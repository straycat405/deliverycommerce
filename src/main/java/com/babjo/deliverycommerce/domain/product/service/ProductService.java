package com.babjo.deliverycommerce.domain.product.service;

import com.babjo.deliverycommerce.global.security.UserPrincipal;
import com.babjo.deliverycommerce.domain.product.dto.ProductCreateRequestDto;
import com.babjo.deliverycommerce.domain.product.dto.ProductResponseDto;
import com.babjo.deliverycommerce.domain.product.dto.ProductUpdateRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {

    // 관리 API (OWNER, MANAGER, MASTER)
    ProductResponseDto create(UUID storeId, ProductCreateRequestDto request, UserPrincipal user);
    ProductResponseDto update(UUID storeId, UUID productId, ProductUpdateRequestDto request, UserPrincipal user);
    void delete(UUID storeId, UUID productId, UserPrincipal user);
    void hide(UUID storeId, UUID productId, UserPrincipal user);
    void show(UUID storeId, UUID productId, UserPrincipal user);

    ProductResponseDto generateDescription(UUID storeId, UUID productId, String point, UserPrincipal user);

    // 조회 API (permitAll)
    ProductResponseDto get(UUID storeId, UUID productId,UserPrincipal user);
    Page<ProductResponseDto> getAll(UUID storeId, String categoryOrNull, UserPrincipal user, Pageable pageable);
}
