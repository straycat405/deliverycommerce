package com.babjo.deliverycommerce.domain.review.controller;

import com.babjo.deliverycommerce.domain.review.dto.ReviewCreateResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewUpdateResponse;
import com.babjo.deliverycommerce.domain.review.service.ReviewService;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.babjo.deliverycommerce.global.exception.GlobalExceptionHandler;
import com.babjo.deliverycommerce.global.security.CurrentUserResolver;
import com.babjo.deliverycommerce.global.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ReviewController 단위 테스트
 *
 * [핵심 설계 결정]
 * - standaloneSetup: Spring Context 없이 Controller 레이어만 격리 테스트
 * - 인증 주입: MockHttpServletRequest의 userPrincipal을 직접 세팅하는 RequestPostProcessor를
 *   사용하여 Spring MVC가 Authentication 파라미터를 올바르게 주입하도록 처리
 * - GlobalExceptionHandler: setControllerAdvice()로 명시 등록하여 예외 → HTTP 상태 변환 검증
 */
@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    private MockMvc mockMvc;

    @Mock private ReviewService reviewService;
    @Mock private CurrentUserResolver currentUserResolver;

    @InjectMocks
    private ReviewController reviewController;

    private static final Long   CUSTOMER_ID   = 1L;
    private static final String CUSTOMER_ROLE = "ROLE_CUSTOMER";

    /** 각 MockMvc 요청에 Authentication을 주입하는 RequestPostProcessor */
    private RequestPostProcessor withCustomerAuth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        UserPrincipal customerPrincipal = new UserPrincipal(CUSTOMER_ID, "testuser", CUSTOMER_ROLE);
        Authentication customerAuth = new UsernamePasswordAuthenticationToken(
                customerPrincipal, null,
                List.of(new SimpleGrantedAuthority(CUSTOMER_ROLE))
        );

        // MockHttpServletRequest에 직접 UserPrincipal을 세팅
        // → Spring MVC HandlerMethodArgumentResolver가 Authentication 파라미터로 변환해 주입
        withCustomerAuth = request -> {
            request.setUserPrincipal(customerAuth);
            return request;
        };

        lenient().when(currentUserResolver.getUserId(any(Authentication.class)))
                .thenReturn(CUSTOMER_ID);
    }

    // ─────────────────────────────────────────────────────────────────
    // POST /v1/reviews
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /v1/reviews — 리뷰 생성")
    class CreateReview {

        @Test
        @DisplayName("성공 — 정상 요청 시 201과 응답 바디 반환")
        void createReview_success() throws Exception {
            UUID storeId  = UUID.randomUUID();
            UUID reviewId = UUID.randomUUID();
            UUID orderId  = UUID.randomUUID();
            ReviewCreateResponse response = ReviewCreateResponse.builder()
                    .reviewId(reviewId).userId(CUSTOMER_ID).storeId(storeId)
                    .rating(4).content("good").createdAt(LocalDateTime.now()).build();

            when(reviewService.createReview(eq(CUSTOMER_ID), any())).thenReturn(response);

            mockMvc.perform(post("/v1/reviews")
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format(
                                    "{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":4,\"content\":\"good\"}", orderId, storeId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.reviewId").value(reviewId.toString()))
                    .andExpect(jsonPath("$.data.userId").value(CUSTOMER_ID.intValue()))
                    .andExpect(jsonPath("$.data.rating").value(4))
                    .andExpect(jsonPath("$.data.content").value("good"));

            verify(reviewService, times(1)).createReview(eq(CUSTOMER_ID), any());
        }

        @Test
        @DisplayName("성공 — rating 경계값 최솟값 1")
        void createReview_boundary_rating_min_1() throws Exception {
            UUID storeId  = UUID.randomUUID();
            UUID reviewId = UUID.randomUUID();
            UUID orderId  = UUID.randomUUID();
            ReviewCreateResponse response = ReviewCreateResponse.builder()
                    .reviewId(reviewId).userId(CUSTOMER_ID).storeId(storeId)
                    .rating(1).content("bad").createdAt(LocalDateTime.now()).build();

            when(reviewService.createReview(eq(CUSTOMER_ID), any())).thenReturn(response);

            mockMvc.perform(post("/v1/reviews")
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format(
                                    "{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":1,\"content\":\"bad\"}", orderId, storeId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.rating").value(1));
        }

        @Test
        @DisplayName("성공 — rating 경계값 최댓값 5")
        void createReview_boundary_rating_max_5() throws Exception {
            UUID storeId  = UUID.randomUUID();
            UUID reviewId = UUID.randomUUID();
            UUID orderId  = UUID.randomUUID();
            ReviewCreateResponse response = ReviewCreateResponse.builder()
                    .reviewId(reviewId).userId(CUSTOMER_ID).storeId(storeId)
                    .rating(5).content("great").createdAt(LocalDateTime.now()).build();

            when(reviewService.createReview(eq(CUSTOMER_ID), any())).thenReturn(response);

            mockMvc.perform(post("/v1/reviews")
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format(
                                    "{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":5,\"content\":\"great\"}", orderId, storeId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.rating").value(5));
        }

        @Test
        @DisplayName("검증 실패 — 필수 필드 누락 시 400")
        void createReview_validation_fail_missing_required_fields() throws Exception {
            mockMvc.perform(post("/v1/reviews")
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
            verify(reviewService, never()).createReview(any(), any());
        }

        @Test
        @DisplayName("검증 실패 — rating 6 초과 시 400")
        void createReview_validation_fail_rating_above_5() throws Exception {
            mockMvc.perform(post("/v1/reviews")
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format(
                                    "{\"storeId\":\"%s\",\"rating\":6,\"content\":\"good\"}",
                                    UUID.randomUUID())))
                    .andExpect(status().isBadRequest());
            verify(reviewService, never()).createReview(any(), any());
        }

        @Test
        @DisplayName("검증 실패 — rating 0 이하 시 400")
        void createReview_validation_fail_rating_below_1() throws Exception {
            mockMvc.perform(post("/v1/reviews")
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format(
                                    "{\"storeId\":\"%s\",\"rating\":0,\"content\":\"good\"}",
                                    UUID.randomUUID())))
                    .andExpect(status().isBadRequest());
            verify(reviewService, never()).createReview(any(), any());
        }

        @Test
        @DisplayName("검증 실패 — content 공백 시 400")
        void createReview_validation_fail_content_blank() throws Exception {
            mockMvc.perform(post("/v1/reviews")
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format(
                                    "{\"storeId\":\"%s\",\"rating\":3,\"content\":\"   \"}",
                                    UUID.randomUUID())))
                    .andExpect(status().isBadRequest());
            verify(reviewService, never()).createReview(any(), any());
        }

        @Test
        @DisplayName("검증 실패 — content null 시 400")
        void createReview_validation_fail_content_null() throws Exception {
            mockMvc.perform(post("/v1/reviews")
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format(
                                    "{\"storeId\":\"%s\",\"rating\":3}", UUID.randomUUID())))
                    .andExpect(status().isBadRequest());
            verify(reviewService, never()).createReview(any(), any());
        }

        @Test
        @DisplayName("실패 전파 — USER_NOT_FOUND → 404")
        void createReview_fail_user_not_found_propagates_404() throws Exception {
            when(reviewService.createReview(eq(CUSTOMER_ID), any()))
                    .thenThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

            mockMvc.perform(post("/v1/reviews")
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format(
                                    "{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":4,\"content\":\"good\"}",
                                    UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isNotFound());
            verify(reviewService, times(1)).createReview(eq(CUSTOMER_ID), any());
        }

        @Test
        @DisplayName("실패 전파 — STORE_NOT_FOUND → 404")
        void createReview_fail_store_not_found_propagates_404() throws Exception {
            when(reviewService.createReview(eq(CUSTOMER_ID), any()))
                    .thenThrow(new CustomException(ErrorCode.STORE_NOT_FOUND));

            mockMvc.perform(post("/v1/reviews")
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format(
                                    "{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":4,\"content\":\"good\"}",
                                    UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isNotFound());
            verify(reviewService, times(1)).createReview(eq(CUSTOMER_ID), any());
        }

        @Test
        @DisplayName("실패 전파 — REVIEW_ALREADY_EXISTS → 409")
        void createReview_fail_duplicate_review_propagates_409() throws Exception {
            when(reviewService.createReview(eq(CUSTOMER_ID), any()))
                    .thenThrow(new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS));

            mockMvc.perform(post("/v1/reviews")
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format(
                                    "{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":4,\"content\":\"good\"}",
                                    UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isConflict());
            verify(reviewService, times(1)).createReview(eq(CUSTOMER_ID), any());
        }

        @Test
        @DisplayName("실패 전파 — ORDER_NOT_FOUND → 404")
        void createReview_fail_order_not_found_propagates_404() throws Exception {
            when(reviewService.createReview(eq(CUSTOMER_ID), any()))
                    .thenThrow(new CustomException(ErrorCode.ORDER_NOT_FOUND));

            mockMvc.perform(post("/v1/reviews")
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format(
                                    "{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":4,\"content\":\"good\"}",
                                    UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isNotFound());
            verify(reviewService, times(1)).createReview(eq(CUSTOMER_ID), any());
        }

        @Test
        @DisplayName("실패 전파 — ORDER_NOT_COMPLETED → 400")
        void createReview_fail_order_not_completed_propagates_400() throws Exception {
            when(reviewService.createReview(eq(CUSTOMER_ID), any()))
                    .thenThrow(new CustomException(ErrorCode.ORDER_NOT_COMPLETED));

            mockMvc.perform(post("/v1/reviews")
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format(
                                    "{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":4,\"content\":\"good\"}",
                                    UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isBadRequest());
            verify(reviewService, times(1)).createReview(eq(CUSTOMER_ID), any());
        }

        @Test
        @DisplayName("실패 전파 — 예상치 못한 예외 → 500")
        void createReview_fail_unexpected_error_returns_500() throws Exception {
            when(reviewService.createReview(eq(CUSTOMER_ID), any()))
                    .thenThrow(new RuntimeException("unexpected"));

            mockMvc.perform(post("/v1/reviews")
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format(
                                    "{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":4,\"content\":\"good\"}",
                                    UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // PUT /v1/reviews/{reviewId}
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PUT /v1/reviews/{reviewId} — 리뷰 수정")
    class UpdateReview {

        @Test
        @DisplayName("성공 — 본인 리뷰 수정 시 200과 응답 바디 반환")
        void updateReview_success() throws Exception {
            UUID reviewId = UUID.randomUUID();
            ReviewUpdateResponse response = ReviewUpdateResponse.builder()
                    .reviewId(reviewId).rating(5).content("very good")
                    .updatedAt(LocalDateTime.now()).build();

            when(reviewService.updateReview(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), eq(reviewId), any()))
                    .thenReturn(response);

            mockMvc.perform(put("/v1/reviews/" + reviewId)
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rating\":5,\"content\":\"very good\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reviewId").value(reviewId.toString()))
                    .andExpect(jsonPath("$.data.rating").value(5))
                    .andExpect(jsonPath("$.data.content").value("very good"));

            verify(reviewService, times(1))
                    .updateReview(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), eq(reviewId), any());
        }

        @Test
        @DisplayName("실패 전파 — 권한 없음 → 403")
        void updateReview_fail_not_owner_propagates_403() throws Exception {
            UUID reviewId = UUID.randomUUID();
            when(reviewService.updateReview(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), eq(reviewId), any()))
                    .thenThrow(new CustomException(ErrorCode.REVIEW_FORBIDDEN));

            mockMvc.perform(put("/v1/reviews/" + reviewId)
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rating\":5,\"content\":\"good\"}"))
                    .andExpect(status().isForbidden());

            verify(reviewService, times(1))
                    .updateReview(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), eq(reviewId), any());
        }

        @Test
        @DisplayName("검증 실패 — rating 0 이하 시 400")
        void updateReview_validation_fail_rating_below_1() throws Exception {
            UUID reviewId = UUID.randomUUID();
            mockMvc.perform(put("/v1/reviews/" + reviewId)
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rating\":0,\"content\":\"bad\"}"))
                    .andExpect(status().isBadRequest());
            verify(reviewService, never()).updateReview(any(), any(), any(), any());
        }

        @Test
        @DisplayName("검증 실패 — rating 6 초과 시 400")
        void updateReview_validation_fail_rating_above_5() throws Exception {
            UUID reviewId = UUID.randomUUID();
            mockMvc.perform(put("/v1/reviews/" + reviewId)
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rating\":6,\"content\":\"too much\"}"))
                    .andExpect(status().isBadRequest());
            verify(reviewService, never()).updateReview(any(), any(), any(), any());
        }

        @Test
        @DisplayName("실패 전파 — REVIEW_NOT_FOUND → 404")
        void updateReview_fail_review_not_found_propagates_404() throws Exception {
            UUID reviewId = UUID.randomUUID();
            when(reviewService.updateReview(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), eq(reviewId), any()))
                    .thenThrow(new CustomException(ErrorCode.REVIEW_NOT_FOUND));

            mockMvc.perform(put("/v1/reviews/" + reviewId)
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rating\":4,\"content\":\"good\"}"))
                    .andExpect(status().isNotFound());

            verify(reviewService, times(1))
                    .updateReview(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), eq(reviewId), any());
        }

        @Test
        @DisplayName("성공 — rating만 수정 (content 미포함)")
        void updateReview_only_rating_changed() throws Exception {
            UUID reviewId = UUID.randomUUID();
            ReviewUpdateResponse response = ReviewUpdateResponse.builder()
                    .reviewId(reviewId).rating(3).content("기존 내용")
                    .updatedAt(LocalDateTime.now()).build();

            when(reviewService.updateReview(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), eq(reviewId), any()))
                    .thenReturn(response);

            mockMvc.perform(put("/v1/reviews/" + reviewId)
                            .with(withCustomerAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rating\":3}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.rating").value(3));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /v1/reviews
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /v1/reviews — 리뷰 조회")
    class GetReviews {

        @Test
        @DisplayName("파라미터 없음 — CUSTOMER 본인 리뷰 목록 반환")
        void getReviews_customer_no_params_returns_own_reviews() throws Exception {
            ReviewResponse reviewResponse = ReviewResponse.builder()
                    .userId(CUSTOMER_ID).rating(4).content("good").build();
            Page<ReviewResponse> page = new PageImpl<>(
                    List.of(reviewResponse), PageRequest.of(0, 10), 1);

            when(reviewService.getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/v1/reviews").with(withCustomerAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].userId").value(CUSTOMER_ID));

            verify(reviewService, times(1))
                    .getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any());
        }

        @Test
        @DisplayName("파라미터 없음 — 빈 목록 반환")
        void getReviews_no_params_empty_list() throws Exception {
            Page<ReviewResponse> emptyPage = new PageImpl<>(
                    Collections.emptyList(), PageRequest.of(0, 10), 0);

            when(reviewService.getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any()))
                    .thenReturn(emptyPage);

            mockMvc.perform(get("/v1/reviews").with(withCustomerAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @DisplayName("reviewId 파라미터 — 단건 조회 성공")
        void getReviews_by_reviewId_success() throws Exception {
            UUID reviewId = UUID.randomUUID();
            UUID storeId  = UUID.randomUUID();
            ReviewResponse reviewResponse = ReviewResponse.builder()
                    .reviewId(reviewId).userId(CUSTOMER_ID).storeId(storeId)
                    .rating(5).content("good").build();
            Page<ReviewResponse> page = new PageImpl<>(
                    List.of(reviewResponse), PageRequest.of(0, 10), 1);

            when(reviewService.getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/v1/reviews")
                            .with(withCustomerAuth)
                            .param("reviewId", reviewId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].reviewId").value(reviewId.toString()))
                    .andExpect(jsonPath("$.data.content[0].rating").value(5));
        }

        @Test
        @DisplayName("reviewId 파라미터 — 타인 리뷰 조회 → 403")
        void getReviews_by_reviewId_forbidden_propagates_403() throws Exception {
            UUID reviewId = UUID.randomUUID();
            when(reviewService.getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any()))
                    .thenThrow(new CustomException(ErrorCode.REVIEW_FORBIDDEN));

            mockMvc.perform(get("/v1/reviews")
                            .with(withCustomerAuth)
                            .param("reviewId", reviewId.toString()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("reviewId 파라미터 — 없는 리뷰 조회 → 404")
        void getReviews_by_reviewId_not_found_propagates_404() throws Exception {
            UUID reviewId = UUID.randomUUID();
            when(reviewService.getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any()))
                    .thenThrow(new CustomException(ErrorCode.REVIEW_NOT_FOUND));

            mockMvc.perform(get("/v1/reviews")
                            .with(withCustomerAuth)
                            .param("reviewId", reviewId.toString()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("storeId 파라미터 — 가게별 목록 반환")
        void getReviews_by_storeId_success() throws Exception {
            UUID storeId  = UUID.randomUUID();
            UUID reviewId = UUID.randomUUID();
            ReviewResponse reviewResponse = ReviewResponse.builder()
                    .reviewId(reviewId).userId(CUSTOMER_ID).storeId(storeId)
                    .rating(3).content("ok").build();
            Page<ReviewResponse> page = new PageImpl<>(
                    List.of(reviewResponse), PageRequest.of(0, 10), 1);

            when(reviewService.getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/v1/reviews")
                            .with(withCustomerAuth)
                            .param("storeId", storeId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].storeId").value(storeId.toString()))
                    .andExpect(jsonPath("$.data.content[0].rating").value(3));
        }

        @Test
        @DisplayName("storeId 파라미터 — 빈 목록 반환")
        void getReviews_by_storeId_empty() throws Exception {
            UUID storeId = UUID.randomUUID();
            Page<ReviewResponse> emptyPage = new PageImpl<>(
                    Collections.emptyList(), PageRequest.of(0, 10), 0);

            when(reviewService.getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any()))
                    .thenReturn(emptyPage);

            mockMvc.perform(get("/v1/reviews")
                            .with(withCustomerAuth)
                            .param("storeId", storeId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @DisplayName("검증 실패 — 허용되지 않은 size(20) → 400")
        void getReviews_invalid_page_size_returns_400() throws Exception {
            mockMvc.perform(get("/v1/reviews")
                            .with(withCustomerAuth)
                            .param("size", "20"))
                    .andExpect(status().isBadRequest());
            verify(reviewService, never()).getReviews(any(), any(), any());
        }

        @Test
        @DisplayName("검증 실패 — 허용되지 않은 sortBy 값 → 400")
        void getReviews_invalid_sortBy_returns_400() throws Exception {
            mockMvc.perform(get("/v1/reviews")
                            .with(withCustomerAuth)
                            .param("sortBy", "invalid"))
                    .andExpect(status().isBadRequest());
            verify(reviewService, never()).getReviews(any(), any(), any());
        }

        @Test
        @DisplayName("검증 실패 — 허용되지 않은 sortDir 값 → 400")
        void getReviews_invalid_sortDir_returns_400() throws Exception {
            mockMvc.perform(get("/v1/reviews")
                            .with(withCustomerAuth)
                            .param("sortDir", "invalid"))
                    .andExpect(status().isBadRequest());
            verify(reviewService, never()).getReviews(any(), any(), any());
        }

        @Test
        @DisplayName("성공 — size=30 허용값으로 조회")
        void getReviews_valid_size_30() throws Exception {
            Page<ReviewResponse> emptyPage = new PageImpl<>(
                    Collections.emptyList(), PageRequest.of(0, 30), 0);

            when(reviewService.getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any()))
                    .thenReturn(emptyPage);

            mockMvc.perform(get("/v1/reviews")
                            .with(withCustomerAuth)
                            .param("size", "30"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("성공 — sortBy=rating, sortDir=asc 정렬 조회")
        void getReviews_sort_by_rating_asc() throws Exception {
            ReviewResponse r1 = ReviewResponse.builder().userId(CUSTOMER_ID).rating(2).build();
            ReviewResponse r2 = ReviewResponse.builder().userId(CUSTOMER_ID).rating(5).build();
            Page<ReviewResponse> page = new PageImpl<>(
                    List.of(r1, r2), PageRequest.of(0, 10), 2);

            when(reviewService.getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/v1/reviews")
                            .with(withCustomerAuth)
                            .param("sortBy", "rating")
                            .param("sortDir", "asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(2)));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE /v1/reviews/{reviewId}
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("DELETE /v1/reviews/{reviewId} — 리뷰 삭제")
    class DeleteReview {

        @Test
        @DisplayName("성공 — 본인 리뷰 삭제 시 200")
        void deleteReview_success() throws Exception {
            UUID reviewId = UUID.randomUUID();
            doNothing().when(reviewService)
                    .deleteReview(eq(reviewId), eq(CUSTOMER_ID), eq(CUSTOMER_ROLE));

            mockMvc.perform(delete("/v1/reviews/" + reviewId)
                            .with(withCustomerAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("리뷰 삭제 성공"));

            verify(reviewService, times(1))
                    .deleteReview(eq(reviewId), eq(CUSTOMER_ID), eq(CUSTOMER_ROLE));
        }

        @Test
        @DisplayName("실패 전파 — 권한 없음 → 403")
        void deleteReview_fail_not_owner_propagates_403() throws Exception {
            UUID reviewId = UUID.randomUUID();
            doThrow(new CustomException(ErrorCode.REVIEW_FORBIDDEN))
                    .when(reviewService)
                    .deleteReview(eq(reviewId), eq(CUSTOMER_ID), eq(CUSTOMER_ROLE));

            mockMvc.perform(delete("/v1/reviews/" + reviewId)
                            .with(withCustomerAuth))
                    .andExpect(status().isForbidden());

            verify(reviewService, times(1))
                    .deleteReview(eq(reviewId), eq(CUSTOMER_ID), eq(CUSTOMER_ROLE));
        }

        @Test
        @DisplayName("실패 전파 — REVIEW_NOT_FOUND → 404")
        void deleteReview_fail_not_found_propagates_404() throws Exception {
            UUID reviewId = UUID.randomUUID();
            doThrow(new CustomException(ErrorCode.REVIEW_NOT_FOUND))
                    .when(reviewService)
                    .deleteReview(eq(reviewId), eq(CUSTOMER_ID), eq(CUSTOMER_ROLE));

            mockMvc.perform(delete("/v1/reviews/" + reviewId)
                            .with(withCustomerAuth))
                    .andExpect(status().isNotFound());

            verify(reviewService, times(1))
                    .deleteReview(eq(reviewId), eq(CUSTOMER_ID), eq(CUSTOMER_ROLE));
        }

        @Test
        @DisplayName("실패 전파 — 예상치 못한 예외 → 500")
        void deleteReview_fail_unexpected_error_returns_500() throws Exception {
            UUID reviewId = UUID.randomUUID();
            doThrow(new RuntimeException("unexpected"))
                    .when(reviewService)
                    .deleteReview(eq(reviewId), eq(CUSTOMER_ID), eq(CUSTOMER_ROLE));

            mockMvc.perform(delete("/v1/reviews/" + reviewId)
                            .with(withCustomerAuth))
                    .andExpect(status().isInternalServerError());
        }
    }
}

