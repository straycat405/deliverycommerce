package com.babjo.deliverycommerce.domain.review.controller;

import com.babjo.deliverycommerce.domain.review.dto.ReviewCreateResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewUpdateResponse;
import com.babjo.deliverycommerce.domain.review.service.ReviewService;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.babjo.deliverycommerce.global.exception.GlobalExceptionHandler;
import com.babjo.deliverycommerce.global.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReviewControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    private UserPrincipal customerPrincipal;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        customerPrincipal = new UserPrincipal(1L, "testuser", "ROLE_CUSTOMER");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                customerPrincipal, null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ───────────────────────────────────────────────
    // POST /v1/reviews - 리뷰 생성
    // ───────────────────────────────────────────────

    @Test
    void createReview_성공() throws Exception {
        UUID orderId  = UUID.randomUUID();
        UUID storeId  = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        ReviewCreateResponse response = ReviewCreateResponse.builder()
                .reviewId(reviewId)
                .userId(1L)
                .orderId(orderId)
                .storeId(storeId)
                .rating(4)
                .content("맛있어요")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewService.createReview(any(UserPrincipal.class), any())).thenReturn(response);

        String requestBody = String.format("""
                {
                  "orderId":  "%s",
                  "storeId":  "%s",
                  "rating":   4,
                  "content":  "맛있어요"
                }
                """, orderId, storeId);

        mockMvc.perform(post("/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reviewId").value(reviewId.toString()))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.data.storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.data.rating").value(4))
                .andExpect(jsonPath("$.data.content").value("맛있어요"));

        verify(reviewService, times(1)).createReview(any(UserPrincipal.class), any());
    }

    @Test
    void createReview_성공_경계값_rating_최솟값_1() throws Exception {
        UUID orderId  = UUID.randomUUID();
        UUID storeId  = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        ReviewCreateResponse response = ReviewCreateResponse.builder()
                .reviewId(reviewId)
                .userId(1L)
                .storeId(storeId)
                .rating(1)
                .content("별로에요")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewService.createReview(any(UserPrincipal.class), any())).thenReturn(response);

        String requestBody = String.format("""
                {
                  "orderId": "%s",
                  "storeId": "%s",
                  "rating":  1,
                  "content": "별로에요"
                }
                """, orderId, storeId);

        mockMvc.perform(post("/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(1));
    }

    @Test
    void createReview_성공_경계값_rating_최댓값_5() throws Exception {
        UUID orderId  = UUID.randomUUID();
        UUID storeId  = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        ReviewCreateResponse response = ReviewCreateResponse.builder()
                .reviewId(reviewId)
                .userId(1L)
                .storeId(storeId)
                .rating(5)
                .content("최고에요")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewService.createReview(any(UserPrincipal.class), any())).thenReturn(response);

        String requestBody = String.format("""
                {
                  "orderId": "%s",
                  "storeId": "%s",
                  "rating":  5,
                  "content": "최고에요"
                }
                """, orderId, storeId);

        mockMvc.perform(post("/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(5));
    }

    @Test
    void createReview_유효성검사_실패_필수필드_누락() throws Exception {
        mockMvc.perform(post("/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    void createReview_유효성검사_실패_rating_범위초과_6() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        String requestBody = String.format("""
                {
                  "orderId":  "%s",
                  "storeId":  "%s",
                  "rating":   6,
                  "content":  "맛있어요"
                }
                """, orderId, storeId);

        mockMvc.perform(post("/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    void createReview_유효성검사_실패_rating_범위미만_0() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        String requestBody = String.format("""
                {
                  "orderId":  "%s",
                  "storeId":  "%s",
                  "rating":   0,
                  "content":  "맛있어요"
                }
                """, orderId, storeId);

        mockMvc.perform(post("/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    void createReview_유효성검사_실패_content_blank() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        String requestBody = String.format("""
                {
                  "orderId":  "%s",
                  "storeId":  "%s",
                  "rating":   3,
                  "content":  "   "
                }
                """, orderId, storeId);

        mockMvc.perform(post("/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    void createReview_유효성검사_실패_content_null() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        String requestBody = String.format("""
                {
                  "orderId":  "%s",
                  "storeId":  "%s",
                  "rating":   3
                }
                """, orderId, storeId);

        mockMvc.perform(post("/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    void createReview_실패_유저없음_예외전파() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        when(reviewService.createReview(any(UserPrincipal.class), any()))
                .thenThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

        String requestBody = String.format("""
                {
                  "orderId": "%s",
                  "storeId": "%s",
                  "rating":  4,
                  "content": "맛있어요"
                }
                """, orderId, storeId);

        mockMvc.perform(post("/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    // ───────────────────────────────────────────────
    // PUT /v1/reviews/{reviewId} - 리뷰 수정
    // ───────────────────────────────────────────────

    @Test
    void updateReview_성공() throws Exception {
        UUID reviewId = UUID.randomUUID();

        ReviewUpdateResponse response = ReviewUpdateResponse.builder()
                .reviewId(reviewId)
                .rating(5)
                .content("정말 맛있어요")
                .updatedAt(LocalDateTime.now())
                .build();

        when(reviewService.updateReview(any(UserPrincipal.class), eq(reviewId), any())).thenReturn(response);

        String requestBody = """
                {
                  "rating":  5,
                  "content": "정말 맛있어요"
                }
                """;

        mockMvc.perform(put("/v1/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewId").value(reviewId.toString()))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.content").value("정말 맛있어요"));

        verify(reviewService, times(1)).updateReview(any(UserPrincipal.class), eq(reviewId), any());
    }

    @Test
    void updateReview_성공_경계값_rating_최댓값_5() throws Exception {
        UUID reviewId = UUID.randomUUID();

        ReviewUpdateResponse response = ReviewUpdateResponse.builder()
                .reviewId(reviewId)
                .rating(5)
                .content("정말 최고에요")
                .updatedAt(LocalDateTime.now())
                .build();

        when(reviewService.updateReview(any(UserPrincipal.class), eq(reviewId), any())).thenReturn(response);

        String requestBody = """
                {
                  "rating":  5,
                  "content": "정말 최고에요"
                }
                """;

        mockMvc.perform(put("/v1/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.content").value("정말 최고에요"));
    }

    @Test
    void updateReview_실패_작성자_불일치_예외전파() throws Exception {
        UUID reviewId = UUID.randomUUID();

        when(reviewService.updateReview(any(UserPrincipal.class), eq(reviewId), any()))
                .thenThrow(new CustomException(ErrorCode.REVIEW_FORBIDDEN));

        String requestBody = """
                {
                  "rating":  5,
                  "content": "정말 맛있어요"
                }
                """;

        mockMvc.perform(put("/v1/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        verify(reviewService, times(1)).updateReview(any(UserPrincipal.class), eq(reviewId), any());
    }


    @Test
    void updateReview_유효성검사_실패_rating_범위미만_0() throws Exception {
        UUID reviewId = UUID.randomUUID();

        String requestBody = """
                {
                  "rating":  0,
                  "content": "별로에요"
                }
                """;

        mockMvc.perform(put("/v1/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).updateReview(any(), any(), any());
    }

    @Test
    void updateReview_유효성검사_실패_rating_범위초과_6() throws Exception {
        UUID reviewId = UUID.randomUUID();

        String requestBody = """
                {
                  "rating":  6,
                  "content": "너무 맛있어요"
                }
                """;

        mockMvc.perform(put("/v1/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).updateReview(any(), any(), any());
    }

    // ───────────────────────────────────────────────
    // GET /v1/reviews - 리뷰 조회
    // ───────────────────────────────────────────────

    @Test
    void getReviews_CUSTOMER_파라미터없음_본인리뷰_반환() throws Exception {
        // standaloneSetup에서 @AuthenticationPrincipal은 SecurityContext의 principal로 주입됨
        ReviewResponse reviewResponse = ReviewResponse.builder()
                .userId(1L)
                .rating(4)
                .content("맛있어요")
                .build();

        when(reviewService.getReviews(any(UserPrincipal.class), eq(null), eq(null)))
                .thenReturn(List.of(reviewResponse));

        mockMvc.perform(get("/v1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].userId").value(1));

        verify(reviewService, times(1)).getReviews(any(UserPrincipal.class), eq(null), eq(null));
    }

    @Test
    void getReviews_CUSTOMER_파라미터없음_빈목록_반환() throws Exception {
        when(reviewService.getReviews(any(UserPrincipal.class), eq(null), eq(null)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/v1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(reviewService, times(1)).getReviews(any(UserPrincipal.class), eq(null), eq(null));
    }

    @Test
    void getReviews_reviewId_단건조회_성공() throws Exception {
        UUID reviewId = UUID.randomUUID();
        UUID storeId  = UUID.randomUUID();

        ReviewResponse reviewResponse = ReviewResponse.builder()
                .reviewId(reviewId)
                .userId(1L)
                .storeId(storeId)
                .rating(5)
                .content("맛있어요")
                .build();

        when(reviewService.getReviews(any(UserPrincipal.class), eq(reviewId), eq(null)))
                .thenReturn(List.of(reviewResponse));

        mockMvc.perform(get("/v1/reviews")
                        .param("reviewId", reviewId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].reviewId").value(reviewId.toString()))
                .andExpect(jsonPath("$.data[0].userId").value(1))
                .andExpect(jsonPath("$.data[0].rating").value(5));

        verify(reviewService, times(1)).getReviews(any(UserPrincipal.class), eq(reviewId), eq(null));
    }

    @Test
    void getReviews_reviewId_타인_리뷰_조회_403_예외전파() throws Exception {
        UUID reviewId = UUID.randomUUID();

        when(reviewService.getReviews(any(UserPrincipal.class), eq(reviewId), eq(null)))
                .thenThrow(new CustomException(ErrorCode.REVIEW_FORBIDDEN));

        mockMvc.perform(get("/v1/reviews")
                        .param("reviewId", reviewId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReviews_reviewId_없는_리뷰_404_예외전파() throws Exception {
        UUID reviewId = UUID.randomUUID();

        when(reviewService.getReviews(any(UserPrincipal.class), eq(reviewId), eq(null)))
                .thenThrow(new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        mockMvc.perform(get("/v1/reviews")
                        .param("reviewId", reviewId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReviews_storeId_필터_성공() throws Exception {
        UUID storeId  = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        ReviewResponse reviewResponse = ReviewResponse.builder()
                .reviewId(reviewId)
                .userId(1L)
                .storeId(storeId)
                .rating(3)
                .content("보통이에요")
                .build();

        when(reviewService.getReviews(any(UserPrincipal.class), eq(null), eq(storeId)))
                .thenReturn(List.of(reviewResponse));

        mockMvc.perform(get("/v1/reviews")
                        .param("storeId", storeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.data[0].userId").value(1))
                .andExpect(jsonPath("$.data[0].rating").value(3))
                .andExpect(jsonPath("$.data[0].content").value("보통이에요"));

        verify(reviewService, times(1)).getReviews(any(UserPrincipal.class), eq(null), eq(storeId));
    }

    @Test
    void getReviews_storeId_필터_결과없음_빈목록_반환() throws Exception {
        UUID storeId = UUID.randomUUID();

        when(reviewService.getReviews(any(UserPrincipal.class), eq(null), eq(storeId)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/v1/reviews")
                        .param("storeId", storeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }


    // ───────────────────────────────────────────────
    // DELETE /v1/reviews/{reviewId} - 리뷰 삭제
    // ───────────────────────────────────────────────

    @Test
    void deleteReview_성공() throws Exception {
        UUID reviewId = UUID.randomUUID();

        doNothing().when(reviewService).deleteReview(eq(reviewId), any(UserPrincipal.class));

        mockMvc.perform(delete("/v1/reviews/" + reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("리뷰 삭제 성공"));

        verify(reviewService, times(1)).deleteReview(eq(reviewId), any(UserPrincipal.class));
    }

    @Test
    void deleteReview_실패_작성자_불일치_예외전파() throws Exception {
        UUID reviewId = UUID.randomUUID();

        doThrow(new CustomException(ErrorCode.REVIEW_FORBIDDEN))
                .when(reviewService).deleteReview(eq(reviewId), any(UserPrincipal.class));

        mockMvc.perform(delete("/v1/reviews/" + reviewId))
                .andExpect(status().isForbidden());

        verify(reviewService, times(1)).deleteReview(eq(reviewId), any(UserPrincipal.class));
    }

    // ───────────────────────────────────────────────
    // 추가 예외 전파 케이스
    // ───────────────────────────────────────────────

    @Test
    void createReview_실패_가게없음_예외전파() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        when(reviewService.createReview(any(UserPrincipal.class), any()))
                .thenThrow(new CustomException(ErrorCode.STORE_NOT_FOUND));

        String requestBody = String.format("""
                {
                  "orderId": "%s",
                  "storeId": "%s",
                  "rating":  4,
                  "content": "맛있어요"
                }
                """, orderId, storeId);

        mockMvc.perform(post("/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());

        verify(reviewService, times(1)).createReview(any(UserPrincipal.class), any());
    }

    @Test
    void createReview_실패_중복리뷰_예외전파() throws Exception {
        // Order 도메인 연결 후 동일 orderId 재등록 시도 → REVIEW_ALREADY_EXISTS
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        when(reviewService.createReview(any(UserPrincipal.class), any()))
                .thenThrow(new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS));

        String requestBody = String.format("""
                {
                  "orderId": "%s",
                  "storeId": "%s",
                  "rating":  4,
                  "content": "맛있어요"
                }
                """, orderId, storeId);

        mockMvc.perform(post("/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());

        verify(reviewService, times(1)).createReview(any(UserPrincipal.class), any());
    }

    @Test
    void deleteReview_실패_존재하지_않는_리뷰_404_예외전파() throws Exception {
        UUID reviewId = UUID.randomUUID();

        doThrow(new CustomException(ErrorCode.REVIEW_NOT_FOUND))
                .when(reviewService).deleteReview(eq(reviewId), any(UserPrincipal.class));

        mockMvc.perform(delete("/v1/reviews/" + reviewId))
                .andExpect(status().isNotFound());

        verify(reviewService, times(1)).deleteReview(eq(reviewId), any(UserPrincipal.class));
    }

    @Test
    void updateReview_실패_존재하지_않는_리뷰_404_예외전파() throws Exception {
        UUID reviewId = UUID.randomUUID();

        when(reviewService.updateReview(any(UserPrincipal.class), eq(reviewId), any()))
                .thenThrow(new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        String requestBody = """
                {
                  "rating":  4,
                  "content": "맛있어요"
                }
                """;

        mockMvc.perform(put("/v1/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());

        verify(reviewService, times(1)).updateReview(any(UserPrincipal.class), eq(reviewId), any());
    }

    @Test
    void createReview_실패_서버내부오류_500_예외전파() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        when(reviewService.createReview(any(UserPrincipal.class), any()))
                .thenThrow(new RuntimeException("예상치 못한 오류"));

        String requestBody = String.format("""
                {
                  "orderId": "%s",
                  "storeId": "%s",
                  "rating":  4,
                  "content": "맛있어요"
                }
                """, orderId, storeId);

        mockMvc.perform(post("/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError());
    }
}


