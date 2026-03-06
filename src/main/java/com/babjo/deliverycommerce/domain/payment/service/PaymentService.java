package com.babjo.deliverycommerce.domain.payment.service;

import com.babjo.deliverycommerce.domain.payment.entity.Payment;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentHistory;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentStatus;
import com.babjo.deliverycommerce.domain.payment.presentation.dto.request.PaymentCancelRequest;
import com.babjo.deliverycommerce.domain.payment.presentation.dto.request.PaymentConfirmRequest;
import com.babjo.deliverycommerce.domain.payment.presentation.dto.request.PaymentCreateRequest;
import com.babjo.deliverycommerce.domain.payment.presentation.dto.response.*;
import com.babjo.deliverycommerce.domain.payment.repository.PaymentHistoryRepository;
import com.babjo.deliverycommerce.domain.payment.repository.PaymentRepository;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 결제 서비스
 * - 팀룰: 클래스 레벨 @Transactional(readOnly = true), CUD 메서드에 @Transactional 개별 적용
 * - 상태 변경 시마다 PaymentHistory Insert
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;

    // ─────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────

    /**
     * 결제 생성 (READY 상태)
     * - 같은 orderId 로 이미 결제가 존재하면 예외
     */
    @Transactional
    public PaymentCreateResponse createPayment(PaymentCreateRequest request, Long userId) {

        // 이미 결제된 주문 중복 방지
        if (paymentRepository.existsByOrderId(request.getOrderId())) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }

        Payment payment = Payment.create(userId, request.getOrderId(), request.getAmount());
        Payment saved = paymentRepository.save(payment);

        // READY 이력 저장
        PaymentHistory history = saveHistory(saved, PaymentStatus.READY, null, null, null);
        saved.updateLastHistoryId(history.getPaymentHistoryId());

        return PaymentCreateResponse.from(saved);
    }

    // ─────────────────────────────────────────────────────────────────
    // CONFIRM (승인)
    // ─────────────────────────────────────────────────────────────────

    /**
     * 결제 승인: READY → COMPLETED
     */
    @Transactional
    public PaymentConfirmResponse confirmPayment(UUID paymentId, PaymentConfirmRequest request, Long userId) {
        Payment payment = getPaymentOrThrow(paymentId);

        if (payment.getPaymentStatus() != PaymentStatus.READY) {
            throw new CustomException(ErrorCode.PAYMENT_INVALID_STATUS);
        }

        payment.confirm(request.getPgPaymentKey());

        PaymentHistory history = saveHistory(payment, PaymentStatus.COMPLETED,
                request.getPgPaymentKey(), null, payment.getLastHistoryId());
        payment.updateLastHistoryId(history.getPaymentHistoryId());

        return PaymentConfirmResponse.from(payment);
    }

    // ─────────────────────────────────────────────────────────────────
    // FAIL (실패)
    // ─────────────────────────────────────────────────────────────────

    /**
     * 결제 실패: READY → FAILED
     */
    @Transactional
    public PaymentFailResponse failPayment(UUID paymentId, Long userId) {
        Payment payment = getPaymentOrThrow(paymentId);

        if (payment.getPaymentStatus() != PaymentStatus.READY) {
            throw new CustomException(ErrorCode.PAYMENT_INVALID_STATUS);
        }

        payment.fail();

        PaymentHistory history = saveHistory(payment, PaymentStatus.FAILED,
                null, "결제 실패", payment.getLastHistoryId());
        payment.updateLastHistoryId(history.getPaymentHistoryId());

        return PaymentFailResponse.from(payment);
    }

    // ─────────────────────────────────────────────────────────────────
    // CANCEL (취소)
    // ─────────────────────────────────────────────────────────────────

    /**
     * 결제 취소: READY 또는 COMPLETED → CANCELED
     * - 결제 생성 5분 이내에만 취소 가능 (요구사항: 주문 취소 5분 이내 제한을 결제에도 적용)
     * - CUSTOMER: 본인 결제만 취소 가능
     * - MANAGER/MASTER: 모든 결제 취소 가능
     */
    @Transactional
    public PaymentCancelResponse cancelPayment(UUID paymentId, PaymentCancelRequest request,
                                               Long userId, boolean isAdmin) {
        Payment payment = getPaymentOrThrow(paymentId);

        // 본인 결제 확인 (관리자는 제외)
        if (!isAdmin && !payment.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.PAYMENT_FORBIDDEN);
        }

        // 취소 가능 상태 체크
        if (payment.getPaymentStatus() == PaymentStatus.CANCELED) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_CANCELED);
        }
        if (payment.getPaymentStatus() == PaymentStatus.FAILED) {
            throw new CustomException(ErrorCode.PAYMENT_INVALID_STATUS);
        }

        // 5분 이내 취소 가능 체크
        if (payment.getCreatedAt() != null &&
                payment.getCreatedAt().plusMinutes(5).isBefore(java.time.LocalDateTime.now())) {
            throw new CustomException(ErrorCode.PAYMENT_CANCEL_TIME_EXPIRED);
        }

        payment.cancel();

        String reason = (request != null && request.getReason() != null)
                ? request.getReason() : "사용자 취소";

        PaymentHistory history = saveHistory(payment, PaymentStatus.CANCELED,
                payment.getPgPaymentKey(), reason, payment.getLastHistoryId());
        payment.updateLastHistoryId(history.getPaymentHistoryId());

        return PaymentCancelResponse.from(payment);
    }

    // ─────────────────────────────────────────────────────────────────
    // READ / SEARCH
    // ─────────────────────────────────────────────────────────────────

    /**
     * 조건 기반 동적 결제 목록 조회 (QueryDSL)
     * - CUSTOMER/OWNER: 본인 결제만 조회 (userId 필터 적용)
     * - MANAGER/MASTER: 전체 조회 (userId 필터 없음)
     */
    public List<PaymentResponse> searchPayments(UUID paymentId, UUID orderId,
                                                PaymentStatus paymentStatus,
                                                Long userId, boolean isAdmin) {
        // 관리자면 userId 필터 없이 전체 조회, 그 외는 본인 결제만
        Long filterUserId = isAdmin ? null : userId;

        return paymentRepository.searchPayments(paymentId, orderId, paymentStatus, filterUserId)
                .stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE (Soft Delete)
    // ─────────────────────────────────────────────────────────────────

    /**
     * 결제 기록 Soft Delete
     * - CUSTOMER/OWNER: 본인 결제만 삭제 가능
     * - MANAGER/MASTER: 모든 결제 삭제 가능
     */
    @Transactional
    public PaymentDeleteResponse deletePayment(UUID paymentId, Long userId, boolean isAdmin) {
        Payment payment = getPaymentOrThrow(paymentId);

        if (!isAdmin && !payment.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.PAYMENT_FORBIDDEN);
        }

        if (payment.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_DELETED);
        }

        payment.delete(userId);

        return PaymentDeleteResponse.of(paymentId);
    }

    // ─────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ─────────────────────────────────────────────────────────────────

    private Payment getPaymentOrThrow(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    /**
     * 결제 이력 저장 (Insert-Only)
     */
    private PaymentHistory saveHistory(Payment payment, PaymentStatus eventType,
                                       String pgPaymentKey, String reason,
                                       UUID previousHistoryId) {
        PaymentHistory history = PaymentHistory.create(
                payment.getPaymentId(),
                eventType,
                payment.getAmount(),
                pgPaymentKey,
                reason,
                previousHistoryId
        );
        return paymentHistoryRepository.save(history);
    }
}

