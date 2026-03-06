package com.babjo.deliverycommerce.domain.product.controller;

import com.babjo.deliverycommerce.global.security.UserPrincipal;
import com.babjo.deliverycommerce.domain.product.dto.AiDescriptionRequestDto;
import com.babjo.deliverycommerce.domain.product.dto.ProductCreateRequestDto;
import com.babjo.deliverycommerce.domain.product.dto.ProductResponseDto;
import com.babjo.deliverycommerce.domain.product.dto.ProductUpdateRequestDto;
import com.babjo.deliverycommerce.domain.product.service.ProductService;
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
@RequestMapping("/v1/products/{storeId}")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
public class ProductController {

    private final ProductService productService;

    /* =========================
       조회 API (permitAll)
     ========================= */

    @PreAuthorize("permitAll()")
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> get(
            @PathVariable UUID storeId, @PathVariable UUID productId, @AuthenticationPrincipal UserPrincipal user) {
        ProductResponseDto response = productService.get(storeId, productId, user);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("permitAll()")
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getProducts(
            @PathVariable UUID storeId, @RequestParam(required = false) String category, @AuthenticationPrincipal UserPrincipal user) {

        return ResponseEntity.ok(productService.getAll(storeId, category, user));
    }

    /* =========================
       관리 API (OWNER/MANAGER/MASTER)
     ========================= */

    @PostMapping
    public ResponseEntity<ProductResponseDto> create(
            @PathVariable UUID storeId, @Valid @RequestBody ProductCreateRequestDto request,
            @AuthenticationPrincipal UserPrincipal user)
    {
        ProductResponseDto response = productService.create(storeId, request, user);
        return ResponseEntity.ok(response);
    }



    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> update(
            @PathVariable UUID storeId, @PathVariable UUID productId,
            @Valid @RequestBody ProductUpdateRequestDto request, @AuthenticationPrincipal UserPrincipal user)
    {
        ProductResponseDto response = productService.update(storeId, productId, request, user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID storeId, @PathVariable UUID productId,
            @AuthenticationPrincipal UserPrincipal user)
    {
        productService.delete(storeId, productId, user);
        return ResponseEntity.noContent().build();  // 성공적으로 삭제 시 204 No Content
    }

    @PostMapping("/{productId}/ai-description")
    public ResponseEntity<ProductResponseDto> generateAiDescription(
            @PathVariable UUID storeId, @PathVariable UUID productId,
            @RequestBody AiDescriptionRequestDto request, @AuthenticationPrincipal UserPrincipal user)
    {
        return ResponseEntity.ok(
                productService.generateDescription(storeId, productId, request.getPoint(), user)
        );
    }

    // hide, show 를 좋아요, 좋아요 취소 처럼 하나의 API로 수정 예정
    @PatchMapping("/{productId}/hide")
    public ResponseEntity<Void> hide(
            @PathVariable UUID storeId, @PathVariable UUID productId, @AuthenticationPrincipal UserPrincipal user) {
        productService.hide(storeId, productId, user);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{productId}/show")
    public ResponseEntity<Void> show(
            @PathVariable UUID storeId, @PathVariable UUID productId, @AuthenticationPrincipal UserPrincipal user) {
        productService.show(storeId, productId, user);
        return ResponseEntity.noContent().build();
    }


}
