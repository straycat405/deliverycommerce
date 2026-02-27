package com.babjo.deliverycommerce.product.controller;

import com.babjo.deliverycommerce.product.dto.ProductCreateRequestDto;
import com.babjo.deliverycommerce.product.dto.ProductResponseDto;
import com.babjo.deliverycommerce.product.dto.ProductUpdateRequestDto;
import com.babjo.deliverycommerce.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/products/")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDto> create(@Valid @RequestBody ProductCreateRequestDto request) {
        ProductResponseDto response = productService.create(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> get(@PathVariable UUID productId) {
        ProductResponseDto response = productService.get(productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAll() {
        List<ProductResponseDto> response = productService.getAll();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> update(@PathVariable UUID productId, @Valid @RequestBody ProductUpdateRequestDto request) {
        ProductResponseDto response = productService.update(productId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(@PathVariable UUID productId) {

        // 임시 UserId
        Long tempUserId = 1L;

        productService.delete(productId, tempUserId);
        return ResponseEntity.noContent().build();  // 성공적으로 삭제 시 204 No Content
    }
}
