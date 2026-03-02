package com.babjo.deliverycommerce.product.service;

import com.babjo.deliverycommerce.product.dto.ProductCreateRequestDto;
import com.babjo.deliverycommerce.product.dto.ProductResponseDto;
import com.babjo.deliverycommerce.product.dto.ProductUpdateRequestDto;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    // Store 이후 연결
    ProductResponseDto create(ProductCreateRequestDto request);
    ProductResponseDto get(UUID productId);
    List<ProductResponseDto> getAll();
    ProductResponseDto update(UUID productId, ProductUpdateRequestDto request);
    void delete(UUID productId, Long userId);

    void hide(UUID productId);
    void show(UUID productId);
}
