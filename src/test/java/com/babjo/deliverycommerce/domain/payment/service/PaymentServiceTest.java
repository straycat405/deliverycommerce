package com.babjo.deliverycommerce.domain.payment.service;

import com.babjo.deliverycommerce.domain.payment.dto.response.*;
import com.babjo.deliverycommerce.domain.payment.entity.Payment;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentHistory;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentStatus;
import com.babjo.deliverycommerce.domain.payment.dto.request.PaymentConfirmRequest;
import com.babjo.deliverycommerce.domain.payment.dto.request.PaymentCreateRequest;
import com.babjo.deliverycommerce.domain.payment.repository.PaymentHistoryRepository;
import com.babjo.deliverycommerce.domain.payment.repository.PaymentRepository;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 단위 테스트")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    @InjectMocks
    private PaymentService paymentService;

    private UUID paymentId;
    private UUID orderId;
    private Long userId;
    private Payment payment;
    private PaymentHistory mockHistory;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        userId = 1L;
        payment = Payment.create(userId, orderId, 15000);
        mockHistory = PaymentHistory.create(paymentId, PaymentStatus.READY, 15000, null, null, null);
    }

    // ─────────────────────────────────────────────────────────────────
    // 결제 생성
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("결제 생성 (createPayment)")
    class CreatePayment {

        @Test
        @DisplayName("정상 케이스 - READY 상태로 결제 생성 성공")
        void createPayment_success() {
            // given
            PaymentCreateRequest request = createRequest(orderId, 15000);
            given(paymentRepository.existsByOrderId(orderId)).willReturn(false);
            given(paymentRepository.save(any(Payment.class))).willReturn(payment);
            given(paymentHistoryRepository.save(any(PaymentHistory.class))).willReturn(mockHistory);

            // when
            PaymentCreateResponse response = paymentService.createPayment(request, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.READY);
            then(paymentRepository).should().save(any(Payment.class));
            then(paymentHistoryRepository).should().save(any(PaymentHistory.class));
        }

        @Test
        @DisplayName("실패 케이스 - 이미 결제된 주문이면 예외 발생")
        void createPayment_fail_alreadyExists() {
            // given
            PaymentCreateRequest request = createRequest(orderId, 15000);
            given(paymentRepository.existsByOrderId(orderId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> paymentService.createPayment(request, userId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.PAYMENT_ALREADY_EXISTS.getMessage());
        }

        private PaymentCreateRequest createRequest(UUID orderId, int amount) {
            try {
                PaymentCreateRequest req = new PaymentCreateRequest();
                var orderIdField = PaymentCreateRequest.class.getDeclaredField("orderId");
                var amountField = PaymentCreateRequest.class.getDeclaredField("amount");
                orderIdField.setAccessible(true);
                amountField.setAccessible(true);
                orderIdField.set(req, orderId);
                amountField.set(req, amount);
                return req;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 결제 승인
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("결제 승인 (confirmPayment)")
    class ConfirmPayment {

        @Test
        @DisplayName("정상 케이스 - READY → COMPLETED 성공")
        void confirmPayment_success() {
            // given
            PaymentConfirmRequest request = createConfirmRequest("pg_key_123");
            given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
            given(paymentHistoryRepository.save(any(PaymentHistory.class))).willReturn(mockHistory);

            // when
            PaymentConfirmResponse response = paymentService.confirmPayment(paymentId, request, userId);

            // then
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }

        @Test
        @DisplayName("실패 케이스 - READY 가 아닌 상태에서 승인 시도시 예외")
        void confirmPayment_fail_invalidStatus() {
            // given
            payment.fail(); // FAILED 상태로 변경
            PaymentConfirmRequest request = createConfirmRequest("pg_key_123");
            given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

            // when & then
            assertThatThrownBy(() -> paymentService.confirmPayment(paymentId, request, userId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.PAYMENT_INVALID_STATUS.getMessage());
        }

        @Test
        @DisplayName("실패 케이스 - 존재하지 않는 paymentId 로 승인 시도시 예외")
        void confirmPayment_fail_notFound() {
            // given
            PaymentConfirmRequest request = createConfirmRequest("pg_key_123");
            given(paymentRepository.findById(paymentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentService.confirmPayment(paymentId, request, userId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.PAYMENT_NOT_FOUND.getMessage());
        }

        private PaymentConfirmRequest createConfirmRequest(String key) {
            try {
                PaymentConfirmRequest req = new PaymentConfirmRequest();
                var field = PaymentConfirmRequest.class.getDeclaredField("pgPaymentKey");
                field.setAccessible(true);
                field.set(req, key);
                return req;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 결제 실패
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("결제 실패 (failPayment)")
    class FailPayment {

        @Test
        @DisplayName("정상 케이스 - READY → FAILED 성공")
        void failPayment_success() {
            // given
            given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
            given(paymentHistoryRepository.save(any(PaymentHistory.class))).willReturn(mockHistory);

            // when
            PaymentFailResponse response = paymentService.failPayment(paymentId, userId);

            // then
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("실패 케이스 - 이미 COMPLETED 상태에서 실패 처리 시 예외")
        void failPayment_fail_invalidStatus() {
            // given
            payment.confirm("pg_key");
            given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

            // when & then
            assertThatThrownBy(() -> paymentService.failPayment(paymentId, userId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.PAYMENT_INVALID_STATUS.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 결제 취소
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("결제 취소 (cancelPayment)")
    class CancelPayment {

        @Test
        @DisplayName("정상 케이스 - READY → CANCELED (5분 이내)")
        void cancelPayment_success() {
            // given
            given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
            given(paymentHistoryRepository.save(any(PaymentHistory.class))).willReturn(mockHistory);

            // when
            PaymentCancelResponse response = paymentService.cancelPayment(paymentId, null, userId, false);

            // then
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        }

        @Test
        @DisplayName("실패 케이스 - 이미 CANCELED 상태에서 취소 시 예외")
        void cancelPayment_fail_alreadyCanceled() {
            // given
            payment.cancel();
            given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

            // when & then
            assertThatThrownBy(() -> paymentService.cancelPayment(paymentId, null, userId, false))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.PAYMENT_ALREADY_CANCELED.getMessage());
        }

        @Test
        @DisplayName("실패 케이스 - 본인 결제가 아닌 경우 취소 시 예외 (비관리자)")
        void cancelPayment_fail_forbidden() {
            // given
            Long otherUserId = 999L;
            given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

            // when & then
            assertThatThrownBy(() -> paymentService.cancelPayment(paymentId, null, otherUserId, false))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.PAYMENT_FORBIDDEN.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 결제 조회
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("결제 조회 (searchPayments)")
    class SearchPayments {

        @Test
        @DisplayName("정상 케이스 - 결제 목록 조회 성공 (일반 사용자)")
        void searchPayments_success_user() {
            // given
            given(paymentRepository.searchPayments(null, null, null, userId))
                    .willReturn(List.of(payment));

            // when
            List<PaymentResponse> responses = paymentService.searchPayments(null, null, null, userId, false);

            // then
            assertThat(responses).hasSize(1);
        }

        @Test
        @DisplayName("정상 케이스 - 관리자는 전체 조회 (userId 필터 없음)")
        void searchPayments_success_admin() {
            // given
            given(paymentRepository.searchPayments(null, null, null, null))
                    .willReturn(List.of(payment));

            // when
            List<PaymentResponse> responses = paymentService.searchPayments(null, null, null, userId, true);

            // then
            assertThat(responses).hasSize(1);
            // 관리자 조회 시 userId=null 로 호출됨을 검증
            then(paymentRepository).should().searchPayments(null, null, null, null);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 결제 삭제
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("결제 삭제 (deletePayment)")
    class DeletePayment {

        @Test
        @DisplayName("정상 케이스 - Soft Delete 성공")
        void deletePayment_success() {
            // given
            given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

            // when
            PaymentDeleteResponse response = paymentService.deletePayment(paymentId, userId, false);

            // then
            assertThat(response.isDeleted()).isTrue();
            assertThat(payment.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("실패 케이스 - 존재하지 않는 결제 삭제 시 예외")
        void deletePayment_fail_notFound() {
            // given
            given(paymentRepository.findById(paymentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentService.deletePayment(paymentId, userId, false))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.PAYMENT_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패 케이스 - Soft Delete 후 재삭제 시 예외")
        void deletePayment_fail_alreadyDeleted() {
            // given
            payment.delete(userId);
            given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

            // when & then
            assertThatThrownBy(() -> paymentService.deletePayment(paymentId, userId, false))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.ALREADY_DELETED.getMessage());
        }
    }
}

