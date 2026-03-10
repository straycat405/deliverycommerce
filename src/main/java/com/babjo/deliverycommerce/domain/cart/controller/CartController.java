package com.babjo.deliverycommerce.domain.cart.controller;


import com.babjo.deliverycommerce.domain.cart.dto.CartItemAddRequestDto;
import com.babjo.deliverycommerce.domain.cart.dto.CartItemQuantityUpdateRequestDto;
import com.babjo.deliverycommerce.domain.cart.dto.CartResponseDto;
import com.babjo.deliverycommerce.domain.cart.service.CartService;
import com.babjo.deliverycommerce.global.common.dto.CommonResponse;
import com.babjo.deliverycommerce.global.security.CurrentUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/cart")
public class CartController {

    private final CartService cartService;
    private final CurrentUserResolver currentUserResolver;

    public CartController(CartService cartService, CurrentUserResolver currentUserResolver) {
        this.cartService = cartService;
        this.currentUserResolver = currentUserResolver;
    }

    @Operation(
            summary = "내 장바구니 조회",
            description = """
                    - 로그인 사용자의 장바구니를 조회 합니다.
                    - 장바구니가 없으면 empty 응답을 반환합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })

    @GetMapping
    public ResponseEntity<CommonResponse<CartResponseDto>> getMyCart(Authentication authentication) {
        Long userId = currentUserResolver.getUserId(authentication);

        log.info("내 장바구니 조회 API 요청: userId={}", userId);

        CartResponseDto response = cartService.getMyCart(userId);

        log.info("내 장바구니 조회 API 완료: userId={}, cartId={}, itemCount={}", userId, response.getCartId(), response.getItems().size());

        return CommonResponse.ok("내 장바구니 조회 성공", response);
    }

    @Operation(
            summary = "장바구니 상품 수량 변경",
            description = """
                    - 특정 장바구니 항목(cartItemId)의 수량을 변경합니다.
                    - quantity는 1 이상이어야 합니다.
                    - 본인 소유 장바구니 항목인지 검증합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패(수량 1 미만 등)"),
            @ApiResponse(responseCode = "403", description = "권한 없음(본인 장바구니 항목 아님)"),
            @ApiResponse(responseCode = "404", description = "장바구니 항목이 존재하지 않음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })

    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<CommonResponse<CartResponseDto>> updateQuantity(@PathVariable UUID cartItemId,
                                                                          @Valid @RequestBody CartItemQuantityUpdateRequestDto request,
                                                                          Authentication authentication) {

        Long userId = currentUserResolver.getUserId(authentication);

        log.info("장바구니 수량 수정 API 요청: userId={},cartItemId={},quantity={}", userId, cartItemId, request.getQuantity());

        CartResponseDto response = cartService.updateQuantity(userId, cartItemId, request);

        log.info("장바구니 수량 수정 API 완료: userId={}, cartId={}, itemCount={}", userId, response.getCartId(), response.getItems().size());

        return CommonResponse.ok("장바구니 수량 수정 성공",response);
    }

    @Operation(
            summary = "장바구니 상품 삭제",
            description = """
                    - 특정 장바구니 항목을 삭제합니다.(Soft Delete)
                    - 본인 소유 장바구니 항목이 아니면 403 반환
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음(본인 장바구니 항목 아님)"),
            @ApiResponse(responseCode = "404", description = "장바구니 항목이 존재하지 않음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CommonResponse<Void>> deleteItem(@PathVariable UUID cartItemId,
                                                           Authentication authentication) {

        Long userId = currentUserResolver.getUserId(authentication);

        log.info("장바구니 항목 삭제 API 요청: userId={}, cartItemId={}", userId, cartItemId);

        cartService.deleteItem(userId, cartItemId);

        log.info("장바구니 항목 삭제 API 완료: userId={}, cartItemId={}", userId, cartItemId);

        return CommonResponse.ok("장바구니 항목 삭제 성공", null);
    }

    @Operation(
            summary = "장바구니 비우기",
            description = """
                    - 로그인 사용자의 장바구니 항목을 전체 삭제합니다.(Soft Delete)
                    - 장바구니가 비어있어도 성공 처리합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비우기 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })

    @DeleteMapping
    public ResponseEntity<CommonResponse<Void>> clearCart(Authentication authentication) {
        Long userId = currentUserResolver.getUserId(authentication);

        log.info("장바구니 비우기 API 요청: userId={}", userId);

        cartService.clearCart(userId);

        log.info("장바구니 비우기 API 완료: userId={}", userId);

        return CommonResponse.ok("장바구니 비우기 성공", null);
    }

    @Operation(
            summary = "장바구니 상품 추가",
            description = """
                    - 로그인 사용자의 장바구니에 상품을 추가합니다.
                    - 동일 상품이 이미 담겨 있으면 수량을 증가시킵니다.
                    - quantity가 null이면 기본값 1로 처리합니다.
                    - 상품이 존재하지 않거나 삭제된 상품이면 404를 반환합니다.
                    - 단일 가게 정책 위반 시 CART_STORE_MISMATCH 예외를 반환합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추가 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패(수량 1 미만 등)"),
            @ApiResponse(responseCode = "404", description = "상품이 존재하지 않음/삭제됨"),
            @ApiResponse(responseCode = "409", description = "단일 가게 정책 위반(CART_STORE_MISMATCH)"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })

    @PostMapping("/items")
    public ResponseEntity<CommonResponse<CartResponseDto>> addItem(@Valid @RequestBody CartItemAddRequestDto request,
                                                                   Authentication authentication) {

        Long userId = currentUserResolver.getUserId(authentication);

        log.info("장바구니 상품 추가 API 요청: userId={}, productId={}, quantity={}", userId, request.getProductId(), request.getQuantity());

        CartResponseDto response = cartService.addItem(userId, request);

        log.info("장바구니 상품 추가 API 완료: userId={}, cartId={}, itemCount={}", userId, response.getCartId(), response.getItems().size());

        return CommonResponse.ok("장바구니 상품 추가 성공", response);

    }
}
