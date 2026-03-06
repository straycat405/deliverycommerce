package com.babjo.deliverycommerce.domain.payment.presentation.controller;

import com.babjo.deliverycommerce.domain.payment.entity.PaymentStatus;
import com.babjo.deliverycommerce.domain.payment.presentation.dto.request.PaymentCancelRequest;
import com.babjo.deliverycommerce.domain.payment.presentation.dto.request.PaymentConfirmRequest;
import com.babjo.deliverycommerce.domain.payment.presentation.dto.request.PaymentCreateRequest;
import com.babjo.deliverycommerce.domain.payment.presentation.dto.response.*;
import com.babjo.deliverycommerce.domain.payment.service.PaymentService;
import com.babjo.deliverycommerce.global.common.dto.CommonResponse;
import com.babjo.deliverycommerce.global.security.CurrentUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 결제 컨트롤러
 * - 팀룰: CurrentUserResolver 로 userId 추출
 * - 팀룰: @PreAuthorize 권한 체크
 * - 팀룰: @Valid DTO 검증
 * - 팀룰: CommonResponse 응답 형식
 */
@Tag(name = "Payment", description = "결제 API")
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUserResolver currentUserResolver;

    // ─────────────────────────────────────────────────────────────────
    // POST /v1/payments  →  결제 생성
    // ─────────────────────────────────────────────────────────────────
    @Operation(summary = "결제 생성", description = "주문에 대한 카드 결제 요청을 생성합니다. (상태: READY)")
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    public ResponseEntity<CommonResponse<PaymentCreateResponse>> createPayment(
            Authentication authentication,
            @Valid @RequestBody PaymentCreateRequest request
    ) {
        Long userId = currentUserResolver.getUserId(authentication);
        return CommonResponse.created("결제 생성 성공", paymentService.createPayment(request, userId));
    }

    // ─────────────────────────────────────────────────────────────────
    // POST /v1/payments/{paymentId}/confirm  →  결제 승인
    // ─────────────────────────────────────────────────────────────────
    @Operation(summary = "결제 승인", description = "PG 승인 완료 처리 (상태: COMPLETED)")
    @PostMapping("/{paymentId}/confirm")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    public ResponseEntity<CommonResponse<PaymentConfirmResponse>> confirmPayment(
            Authentication authentication,
            @PathVariable UUID paymentId,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        Long userId = currentUserResolver.getUserId(authentication);
        return CommonResponse.ok("결제 승인 성공", paymentService.confirmPayment(paymentId, request, userId));
    }

    // ─────────────────────────────────────────────────────────────────
    // PATCH /v1/payments/{paymentId}/fail  →  결제 실패
    // ─────────────────────────────────────────────────────────────────
    @Operation(summary = "결제 실패", description = "결제 실패 상태로 변경합니다. (상태: FAILED)")
    @PatchMapping("/{paymentId}/fail")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    public ResponseEntity<CommonResponse<PaymentFailResponse>> failPayment(
            Authentication authentication,
            @PathVariable UUID paymentId
    ) {
        Long userId = currentUserResolver.getUserId(authentication);
        return CommonResponse.ok("결제 실패 처리 성공", paymentService.failPayment(paymentId, userId));
    }

    // ─────────────────────────────────────────────────────────────────
    // PATCH /v1/payments/{paymentId}/cancel  →  결제 취소
    // ─────────────────────────────────────────────────────────────────
    @Operation(summary = "결제 취소", description = "결제 취소 처리 (생성 후 5분 이내, 상태: CANCELED)")
    @PatchMapping("/{paymentId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    public ResponseEntity<CommonResponse<PaymentCancelResponse>> cancelPayment(
            Authentication authentication,
            @PathVariable UUID paymentId,
            @RequestBody(required = false) @Valid PaymentCancelRequest request
    ) {
        Long userId = currentUserResolver.getUserId(authentication);
        boolean isAdmin = isAdminRole(authentication);
        return CommonResponse.ok("결제 취소 성공", paymentService.cancelPayment(paymentId, request, userId, isAdmin));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /v1/payments  →  결제 조회 (단건/목록 통합)
    // ─────────────────────────────────────────────────────────────────
    @Operation(summary = "결제 조회", description = "paymentId(단건), orderId, paymentStatus 조건 기반 조회")
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    public ResponseEntity<CommonResponse<List<PaymentResponse>>> searchPayments(
            Authentication authentication,
            @RequestParam(required = false) UUID paymentId,
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) PaymentStatus paymentStatus
    ) {
        Long userId = currentUserResolver.getUserId(authentication);
        boolean isAdmin = isAdminRole(authentication);
        return CommonResponse.ok("결제 조회 성공",
                paymentService.searchPayments(paymentId, orderId, paymentStatus, userId, isAdmin));
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE /v1/payments/{paymentId}  →  결제 기록 Soft Delete
    // ─────────────────────────────────────────────────────────────────
    @Operation(summary = "결제 삭제", description = "결제 기록 Soft Delete (deleted_at 세팅)")
    @DeleteMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    public ResponseEntity<CommonResponse<PaymentDeleteResponse>> deletePayment(
            Authentication authentication,
            @PathVariable UUID paymentId
    ) {
        Long userId = currentUserResolver.getUserId(authentication);
        boolean isAdmin = isAdminRole(authentication);
        return CommonResponse.ok("결제 삭제 성공", paymentService.deletePayment(paymentId, userId, isAdmin));
    }

    // ─────────────────────────────────────────────────────────────────
    // 헬퍼: 관리자 역할 확인
    // ─────────────────────────────────────────────────────────────────
    private boolean isAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")
                        || a.getAuthority().equals("ROLE_MASTER"));
    }
}

