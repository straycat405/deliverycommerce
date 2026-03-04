package com.babjo.deliverycommerce.product.service;

import com.babjo.deliverycommerce.global.security.UserPrincipal;
import com.babjo.deliverycommerce.product.dto.ProductCreateRequestDto;
import com.babjo.deliverycommerce.product.dto.ProductResponseDto;
import com.babjo.deliverycommerce.product.dto.ProductUpdateRequestDto;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    // Store 이후 연결
    ProductResponseDto create(ProductCreateRequestDto request);
    ProductResponseDto get(UUID productIdm,UserPrincipal user);
    List<ProductResponseDto> getAll(UserPrincipal user);
    ProductResponseDto update(UUID productId, ProductUpdateRequestDto request);
    void delete(UUID productId, Long userId);

    ProductResponseDto generateDescription(UUID productId, String point);

    void hide(UUID productId);
    void show(UUID productId);
}
