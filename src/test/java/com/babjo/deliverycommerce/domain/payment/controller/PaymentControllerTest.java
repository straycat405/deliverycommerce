package com.babjo.deliverycommerce.domain.payment.controller;

import com.babjo.deliverycommerce.domain.payment.dto.response.*;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentMethod;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentStatus;
import com.babjo.deliverycommerce.domain.payment.entity.PgProvider;
import com.babjo.deliverycommerce.domain.payment.service.PaymentService;
import com.babjo.deliverycommerce.global.security.CurrentUserResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@DisplayName("PaymentController 단위 테스트")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private CurrentUserResolver currentUserResolver;

    // ─────────────────────────────────────────────────────────────────
    // POST /v1/payments  →  결제 생성
    // ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("결제 생성 - 성공 (201 Created)")
    void createPayment_success() throws Exception {
        // given
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        given(currentUserResolver.getUserId(any(Authentication.class))).willReturn(1L);
        given(paymentService.createPayment(any(), eq(1L)))
                .willReturn(PaymentCreateResponse.builder()
                        .paymentId(paymentId)
                        .orderId(orderId)
                        .paymentMethod(PaymentMethod.CARD)
                        .status(PaymentStatus.READY)
                        .createdAt(LocalDateTime.now())
                        .build());

        String body = objectMapper.writeValueAsString(
                new java.util.HashMap<String, Object>() {{
                    put("orderId", orderId.toString());
                    put("amount", 15000);
                }}
        );

        // when & then
        mockMvc.perform(post("/v1/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.paymentMethod").value("CARD"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("결제 생성 - 실패 (amount 누락, 400 Bad Request)")
    void createPayment_fail_missingAmount() throws Exception {
        // given
        String body = "{\"orderId\":\"550e8400-e29b-41d4-a716-446655440000\"}";

        // when & then
        mockMvc.perform(post("/v1/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────────
    // POST /v1/payments/{paymentId}/confirm  →  결제 승인
    // ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("결제 승인 - 성공 (200 OK)")
    void confirmPayment_success() throws Exception {
        // given
        UUID paymentId = UUID.randomUUID();

        given(currentUserResolver.getUserId(any(Authentication.class))).willReturn(1L);
        given(paymentService.confirmPayment(eq(paymentId), any(), eq(1L)))
                .willReturn(PaymentConfirmResponse.builder()
                        .paymentId(paymentId)
                        .status(PaymentStatus.COMPLETED)
                        .approvedAt(LocalDateTime.now())
                        .build());

        String body = "{\"pgPaymentKey\":\"pg_test_123456789\"}";

        // when & then
        mockMvc.perform(post("/v1/payments/{paymentId}/confirm", paymentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    // ─────────────────────────────────────────────────────────────────
    // PATCH /v1/payments/{paymentId}/fail  →  결제 실패
    // ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("결제 실패 처리 - 성공 (200 OK)")
    void failPayment_success() throws Exception {
        // given
        UUID paymentId = UUID.randomUUID();

        given(currentUserResolver.getUserId(any(Authentication.class))).willReturn(1L);
        given(paymentService.failPayment(eq(paymentId), eq(1L)))
                .willReturn(PaymentFailResponse.builder()
                        .paymentId(paymentId)
                        .status(PaymentStatus.FAILED)
                        .updatedAt(LocalDateTime.now())
                        .build());

        // when & then
        mockMvc.perform(patch("/v1/payments/{paymentId}/fail", paymentId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"));
    }

    // ─────────────────────────────────────────────────────────────────
    // PATCH /v1/payments/{paymentId}/cancel  →  결제 취소
    // ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("결제 취소 - 성공 (200 OK)")
    void cancelPayment_success() throws Exception {
        // given
        UUID paymentId = UUID.randomUUID();

        given(currentUserResolver.getUserId(any(Authentication.class))).willReturn(1L);
        given(paymentService.cancelPayment(eq(paymentId), any(), eq(1L), eq(false)))
                .willReturn(PaymentCancelResponse.builder()
                        .paymentId(paymentId)
                        .status(PaymentStatus.CANCELED)
                        .canceledAt(LocalDateTime.now())
                        .build());

        // when & then
        mockMvc.perform(patch("/v1/payments/{paymentId}/cancel", paymentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /v1/payments  →  결제 조회
    // ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("결제 목록 조회 - 성공 (200 OK)")
    void searchPayments_success() throws Exception {
        // given
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        given(currentUserResolver.getUserId(any(Authentication.class))).willReturn(1L);
        given(paymentService.searchPayments(any(), any(), any(), eq(1L), eq(false)))
                .willReturn(List.of(
                        PaymentResponse.builder()
                                .paymentId(paymentId)
                                .userId(1L)
                                .orderId(orderId)
                                .amount(15000)
                                .paymentMethod(PaymentMethod.CARD)
                                .paymentStatus(PaymentStatus.READY)
                                .pgProvider(PgProvider.TOSS)
                                .createdAt(LocalDateTime.now())
                                .createdBy(1L)
                                .build()
                ));

        // when & then
        mockMvc.perform(get("/v1/payments")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].paymentStatus").value("READY"))
                .andExpect(jsonPath("$.data[0].paymentMethod").value("CARD"));
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE /v1/payments/{paymentId}  →  결제 삭제
    // ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("결제 삭제 (Soft Delete) - 성공 (200 OK)")
    void deletePayment_success() throws Exception {
        // given
        UUID paymentId = UUID.randomUUID();

        given(currentUserResolver.getUserId(any(Authentication.class))).willReturn(1L);
        given(paymentService.deletePayment(eq(paymentId), eq(1L), eq(false)))
                .willReturn(PaymentDeleteResponse.of(paymentId));

        // when & then
        mockMvc.perform(delete("/v1/payments/{paymentId}", paymentId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));
    }

    @Test
    @DisplayName("결제 조회 - 미인증 요청 (401 Unauthorized)")
    void searchPayments_fail_unauthorized() throws Exception {
        mockMvc.perform(get("/v1/payments"))
                .andExpect(status().isUnauthorized());
    }
}

