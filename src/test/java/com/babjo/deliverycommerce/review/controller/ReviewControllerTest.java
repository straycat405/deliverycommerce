package com.babjo.deliverycommerce.review.controller;

import com.babjo.deliverycommerce.domain.review.controller.ReviewController;
import com.babjo.deliverycommerce.domain.review.dto.ReviewCreateResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewUpdateResponse;
import com.babjo.deliverycommerce.domain.review.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReviewControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController).build();
    }

    // ───────────────────────────────────────────────
    // POST /v1/reviews - 리뷰 생성
    // ───────────────────────────────────────────────

    @Test
    void createReview_성공() throws Exception {
        UUID orderId  = UUID.randomUUID();
        UUID storeId  = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        ReviewCreateResponse response = new ReviewCreateResponse();
        response.setReviewId(reviewId);
        response.setOrderId(orderId);
        response.setStoreId(storeId);
        response.setRating(4);
        response.setContent("맛있어요");
        response.setCreatedAt(LocalDateTime.now());

        when(reviewService.createReview(any())).thenReturn(response);

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
                .andExpect(jsonPath("$.reviewId").value(reviewId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.content").value("맛있어요"));

        verify(reviewService, times(1)).createReview(any());
    }

    @Test
    void createReview_유효성검사_실패_필수필드_누락() throws Exception {
        // orderId, storeId, rating, content 모두 누락
        mockMvc.perform(post("/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any());
    }

    @Test
    void createReview_유효성검사_실패_rating_범위초과() throws Exception {
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

        verify(reviewService, never()).createReview(any());
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

        verify(reviewService, never()).createReview(any());
    }

    // ───────────────────────────────────────────────
    // PUT /v1/reviews/{reviewId} - 리뷰 수정
    // ───────────────────────────────────────────────

    @Test
    void updateReview_성공() throws Exception {
        UUID reviewId = UUID.randomUUID();

        ReviewUpdateResponse response = new ReviewUpdateResponse();
        response.setReviewId(reviewId);
        response.setRating(5);
        response.setContent("정말 맛있어요");
        response.setUpdatedAt(LocalDateTime.now());

        when(reviewService.updateReview(any(), any())).thenReturn(response);

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
                .andExpect(jsonPath("$.reviewId").value(reviewId.toString()))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.content").value("정말 맛있어요"));

        verify(reviewService, times(1)).updateReview(any(), any());
    }

    @Test
    void updateReview_유효성검사_실패_필수필드_누락() throws Exception {
        UUID reviewId = UUID.randomUUID();

        mockMvc.perform(put("/v1/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).updateReview(any(), any());
    }

    @Test
    void updateReview_유효성검사_실패_rating_범위미만() throws Exception {
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

        verify(reviewService, never()).updateReview(any(), any());
    }

    // ───────────────────────────────────────────────
    // GET /v1/reviews - 리뷰 목록 조회
    // ───────────────────────────────────────────────

    @Test
    void getReviews_전체조회_성공() throws Exception {
        when(reviewService.getReviews(null, null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/v1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(reviewService, times(1)).getReviews(null, null);
    }

    @Test
    void getReviews_storeId_필터_성공() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        ReviewResponse reviewResponse = ReviewResponse.builder()
                .reviewId(reviewId)
                .storeId(storeId)
                .rating(3)
                .content("보통이에요")
                .build();

        when(reviewService.getReviews(null, storeId)).thenReturn(List.of(reviewResponse));

        mockMvc.perform(get("/v1/reviews")
                        .param("storeId", storeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].storeId").value(storeId.toString()))
                .andExpect(jsonPath("$[0].rating").value(3))
                .andExpect(jsonPath("$[0].content").value("보통이에요"));

        verify(reviewService, times(1)).getReviews(null, storeId);
    }

    // ───────────────────────────────────────────────
    // DELETE /v1/reviews/{reviewId} - 리뷰 삭제
    // ───────────────────────────────────────────────

    @Test
    void deleteReview_성공() throws Exception {
        UUID reviewId = UUID.randomUUID();

        doNothing().when(reviewService).deleteReview(any());

        mockMvc.perform(delete("/v1/reviews/" + reviewId))
                .andExpect(status().isOk());

        verify(reviewService, times(1)).deleteReview(reviewId);
    }
}
