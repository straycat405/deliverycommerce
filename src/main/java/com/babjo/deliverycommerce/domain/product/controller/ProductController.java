package com.babjo.deliverycommerce.domain.product.controller;

import com.babjo.deliverycommerce.global.security.UserPrincipal;
import com.babjo.deliverycommerce.domain.product.dto.AiDescriptionRequestDto;
import com.babjo.deliverycommerce.domain.product.dto.ProductCreateRequestDto;
import com.babjo.deliverycommerce.domain.product.dto.ProductResponseDto;
import com.babjo.deliverycommerce.domain.product.dto.ProductUpdateRequestDto;
import com.babjo.deliverycommerce.domain.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/products/{storeId}")
@Tag(name = "product-controller", description = "상품 관련 API")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
public class ProductController {

    private final ProductService productService;

    /* =========================
       조회 API (permitAll)
     ========================= */

    @Operation(
            summary = "상품 단건 조회",
            description = """
                    - 특정 상품을 조회합니다.
                    - 로그인하지 않은 사용자도 조회 가능합니다.
                    - 숨김 상품의 경우 OWNER/MANAGER/MASTER만 조회 가능합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품")
    })
    @SecurityRequirements
    @PreAuthorize("permitAll()")
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> get(
            @PathVariable UUID storeId, @PathVariable UUID productId, @AuthenticationPrincipal UserPrincipal user) {
        ProductResponseDto response = productService.get(storeId, productId, user);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "상품 목록 조회",
            description = """
                    - 특정 매장의 상품 목록을 조회합니다.
                    - category 파라미터로 상품 카테고리 필터링이 가능합니다.
                    - 페이징 조회가 가능합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 목록 조회 성공")
    })
    @SecurityRequirements
    @PreAuthorize("permitAll()")
    @GetMapping
    public ResponseEntity<Page<ProductResponseDto>> getProducts(
            @PathVariable UUID storeId, @RequestParam(required = false) String category, @AuthenticationPrincipal UserPrincipal user,
            Pageable pageable) {

        return ResponseEntity.ok(productService.getAll(storeId, category, user, pageable));
    }

    /* =========================
       관리 API (OWNER/MANAGER/MASTER)
     ========================= */

    @Operation(
            summary = "상품 생성",
            description = """
                    - 새로운 상품을 생성합니다.
                    - [권한] OWNER / MANAGER / MASTER
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 생성 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음")
    })
    @PostMapping
    public ResponseEntity<ProductResponseDto> create(
            @PathVariable UUID storeId, @Valid @RequestBody ProductCreateRequestDto request,
            @AuthenticationPrincipal UserPrincipal user)
    {
        ProductResponseDto response = productService.create(storeId, request, user);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "상품 수정",
            description = """
                    - 특정 상품 정보를 수정합니다.
                    - [권한] OWNER / MANAGER / MASTER
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 수정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품")
    })
    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> update(
            @PathVariable UUID storeId, @PathVariable UUID productId,
            @Valid @RequestBody ProductUpdateRequestDto request, @AuthenticationPrincipal UserPrincipal user)
    {
        ProductResponseDto response = productService.update(storeId, productId, request, user);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "상품 삭제",
            description = """
                    - 특정 상품을 삭제합니다.
                    - Soft Delete 방식으로 처리됩니다.
                    - [권한] OWNER / MANAGER / MASTER
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "상품 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품")
    })
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID storeId, @PathVariable UUID productId,
            @AuthenticationPrincipal UserPrincipal user)
    {
        productService.delete(storeId, productId, user);
        return ResponseEntity.noContent().build();  // 성공적으로 삭제 시 204 No Content
    }

    @Operation(
            summary = "AI 상품 설명 생성",
            description = """
                    - AI를 이용하여 상품 설명을 자동 생성합니다.
                    - 생성된 설명은 상품 description에 저장됩니다.
                    - [권한] OWNER / MANAGER / MASTER
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI 설명 생성 성공"),
            @ApiResponse(responseCode = "404", description = "상품 없음")
    })
    @PostMapping("/{productId}/ai-description")
    public ResponseEntity<ProductResponseDto> generateAiDescription(
            @PathVariable UUID storeId, @PathVariable UUID productId,
            @RequestBody AiDescriptionRequestDto request, @AuthenticationPrincipal UserPrincipal user)
    {
        return ResponseEntity.ok(
                productService.generateDescription(storeId, productId, request.getPoint(), user)
        );
    }

    @Operation(
            summary = "상품 숨김 처리",
            description = """
                    - 상품을 숨김 처리합니다.
                    - 숨김 상품은 일반 사용자에게 노출되지 않습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "숨김 처리 성공")
    })
    @PatchMapping("/{productId}/hide")
    public ResponseEntity<Void> hide(
            @PathVariable UUID storeId, @PathVariable UUID productId, @AuthenticationPrincipal UserPrincipal user) {
        productService.hide(storeId, productId, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "상품 숨김 해제",
            description = """
                    - 숨김 처리된 상품을 다시 노출 상태로 변경합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "숨김 해제 성공")
    })
    @PatchMapping("/{productId}/show")
    public ResponseEntity<Void> show(
            @PathVariable UUID storeId, @PathVariable UUID productId, @AuthenticationPrincipal UserPrincipal user) {
        productService.show(storeId, productId, user);
        return ResponseEntity.noContent().build();
    }


}
