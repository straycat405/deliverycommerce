package com.babjo.deliverycommerce.domain.payment.service;

import com.babjo.deliverycommerce.domain.payment.dto.request.PaymentSearchRequest;
import com.babjo.deliverycommerce.domain.payment.dto.response.*;
import com.babjo.deliverycommerce.domain.payment.entity.Payment;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentHistory;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentStatus;
import com.babjo.deliverycommerce.domain.payment.dto.request.PaymentCancelRequest;
import com.babjo.deliverycommerce.domain.payment.dto.request.PaymentConfirmRequest;
import com.babjo.deliverycommerce.domain.payment.dto.request.PaymentCreateRequest;
import com.babjo.deliverycommerce.domain.payment.repository.PaymentHistoryRepository;
import com.babjo.deliverycommerce.domain.payment.repository.PaymentRepository;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

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
     * - CUSTOMER/OWNER: 본인 결제만 승인 가능
     * - MANAGER/MASTER: 모든 결제 승인 가능
     */
    @Transactional
    public PaymentConfirmResponse confirmPayment(UUID paymentId, PaymentConfirmRequest request,
                                                 Long userId, boolean isAdmin) {
        Payment payment = getPaymentOrThrow(paymentId);

        if (!isAdmin && !payment.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.PAYMENT_FORBIDDEN);
        }

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
     * - CUSTOMER/OWNER: 본인 결제만 실패 처리 가능
     * - MANAGER/MASTER: 모든 결제 실패 처리 가능
     */
    @Transactional
    public PaymentFailResponse failPayment(UUID paymentId, Long userId, boolean isAdmin) {
        Payment payment = getPaymentOrThrow(paymentId);

        if (!isAdmin && !payment.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.PAYMENT_FORBIDDEN);
        }

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
     * - 결제 생성 5분 이내에만 취소 가능 (요구사항)
     * - CUSTOMER/OWNER: 본인 결제만 취소 가능
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
        // createdAt은 JPA persist 이후 자동 세팅되므로 항상 존재해야 함
        // null 방어 대신 null이면 시간 제한 우회되는 허점을 차단
        LocalDateTime createdAt = payment.getCreatedAt();
        if (createdAt == null || createdAt.plusMinutes(5).isBefore(LocalDateTime.now())) {
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
     * 조건 기반 동적 결제 목록 조회 (QueryDSL + Pagination)
     * - CUSTOMER/OWNER: 본인 결제만 조회 (userId 필터 적용)
     * - MANAGER/MASTER: 전체 조회 (userId 필터 없음)
     * - 페이지 사이즈: 10/30/50 (기본값 10, PaymentSearchRequest 검증)
     * - 정렬: createdAt(기본) / amount, asc / desc
     */
    public Page<PaymentResponse> searchPayments(PaymentSearchRequest searchRequest,
                                                Long userId, boolean isAdmin) {
        // 관리자면 userId 필터 없이 전체 조회, 그 외는 본인 결제만
        Long filterUserId = isAdmin ? null : userId;

        return paymentRepository.searchPayments(
                        searchRequest.getPaymentId(),
                        searchRequest.getOrderId(),
                        searchRequest.getPaymentStatus(),
                        filterUserId,
                        searchRequest.toPageable())
                .map(PaymentResponse::from);
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

        // @Where(deleted_at IS NULL)에 의해 findById 자체에서 이미 삭제된 데이터는 조회 안 됨
        // 그러나 명시적 예외 처리로 일관성 유지
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

