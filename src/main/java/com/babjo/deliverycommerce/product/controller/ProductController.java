package com.babjo.deliverycommerce.product.controller;

import com.babjo.deliverycommerce.global.security.UserPrincipal;
import com.babjo.deliverycommerce.product.dto.AiDescriptionRequestDto;
import com.babjo.deliverycommerce.product.dto.ProductCreateRequestDto;
import com.babjo.deliverycommerce.product.dto.ProductResponseDto;
import com.babjo.deliverycommerce.product.dto.ProductUpdateRequestDto;
import com.babjo.deliverycommerce.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/products")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDto> create(@Valid @RequestBody ProductCreateRequestDto request) {
        ProductResponseDto response = productService.create(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> get(@PathVariable UUID productId, @AuthenticationPrincipal UserPrincipal user) {
        ProductResponseDto response = productService.get(productId, user);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("permitAll()")
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAll(@AuthenticationPrincipal UserPrincipal user) {
        List<ProductResponseDto> response = productService.getAll(user);

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

    @PostMapping("/{productId}/ai-description")
    public ResponseEntity<ProductResponseDto> generateAiDescription(@PathVariable UUID productId, @RequestBody AiDescriptionRequestDto request) {
        return ResponseEntity.ok(
                productService.generateDescription(productId, request.getPoint())
        );
    }

    // hide, show 를 좋아요, 좋아요 취소 처럼 하나의 API로 수정 예정
    @PatchMapping("/{productId}/hide")
    public ResponseEntity<Void> hide(@PathVariable UUID productId) {
        productService.hide(productId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{productId}/show")
    public ResponseEntity<Void> show(@PathVariable UUID productId) {
        productService.show(productId);
        return ResponseEntity.noContent().build();
    }


}
