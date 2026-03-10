package com.babjo.deliverycommerce.domain.review.controller;
import com.babjo.deliverycommerce.domain.review.dto.ReviewCreateResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewResponse;
import com.babjo.deliverycommerce.domain.review.dto.ReviewSearchRequest;
import com.babjo.deliverycommerce.domain.review.dto.ReviewUpdateResponse;
import com.babjo.deliverycommerce.domain.review.service.ReviewService;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.babjo.deliverycommerce.global.exception.GlobalExceptionHandler;
import com.babjo.deliverycommerce.global.security.CurrentUserResolver;
import com.babjo.deliverycommerce.global.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
/**
 * ReviewController unit test
 * Team rule: CurrentUserResolver injected as Mock to extract userId
 * Team rule: ReviewService called with Long userId + String role (not UserPrincipal)
 * Response type: Page<ReviewResponse>
 */
class ReviewControllerTest {
    private MockMvc mockMvc;
    @Mock
    private ReviewService reviewService;
    @Mock
    private CurrentUserResolver currentUserResolver;
    @InjectMocks
    private ReviewController reviewController;
    private static final Long   CUSTOMER_ID   = 1L;
    private static final String CUSTOMER_ROLE = "ROLE_CUSTOMER";
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        UserPrincipal customerPrincipal = new UserPrincipal(CUSTOMER_ID, "testuser", CUSTOMER_ROLE);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                customerPrincipal, null,
                List.of(new SimpleGrantedAuthority(CUSTOMER_ROLE))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(currentUserResolver.getUserId(any(Authentication.class))).thenReturn(CUSTOMER_ID);
    }
    // POST /v1/reviews
    @Test
    void createReview_success() throws Exception {
        UUID orderId  = UUID.randomUUID();
        UUID storeId  = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        ReviewCreateResponse response = ReviewCreateResponse.builder()
                .reviewId(reviewId).userId(CUSTOMER_ID).orderId(orderId).storeId(storeId)
                .rating(4).content("good").createdAt(LocalDateTime.now()).build();
        when(reviewService.createReview(eq(CUSTOMER_ID), any())).thenReturn(response);
        String body = String.format("{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":4,\"content\":\"good\"}", orderId, storeId);
        mockMvc.perform(post("/v1/reviews").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reviewId").value(reviewId.toString()))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.rating").value(4))
                .andExpect(jsonPath("$.data.content").value("good"));
        verify(reviewService, times(1)).createReview(eq(CUSTOMER_ID), any());
    }
    @Test
    void createReview_boundary_rating_min_1() throws Exception {
        UUID storeId  = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        ReviewCreateResponse response = ReviewCreateResponse.builder()
                .reviewId(reviewId).userId(CUSTOMER_ID).storeId(storeId)
                .rating(1).content("bad").createdAt(LocalDateTime.now()).build();
        when(reviewService.createReview(eq(CUSTOMER_ID), any())).thenReturn(response);
        String body = String.format("{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":1,\"content\":\"bad\"}", UUID.randomUUID(), storeId);
        mockMvc.perform(post("/v1/reviews").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(1));
    }
    @Test
    void createReview_boundary_rating_max_5() throws Exception {
        UUID storeId  = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        ReviewCreateResponse response = ReviewCreateResponse.builder()
                .reviewId(reviewId).userId(CUSTOMER_ID).storeId(storeId)
                .rating(5).content("great").createdAt(LocalDateTime.now()).build();
        when(reviewService.createReview(eq(CUSTOMER_ID), any())).thenReturn(response);
        String body = String.format("{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":5,\"content\":\"great\"}", UUID.randomUUID(), storeId);
        mockMvc.perform(post("/v1/reviews").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(5));
    }
    @Test
    void createReview_validation_fail_missing_required_fields() throws Exception {
        mockMvc.perform(post("/v1/reviews").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verify(reviewService, never()).createReview(any(), any());
    }
    @Test
    void createReview_validation_fail_rating_above_5() throws Exception {
        String body = String.format("{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":6,\"content\":\"good\"}", UUID.randomUUID(), UUID.randomUUID());
        mockMvc.perform(post("/v1/reviews").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verify(reviewService, never()).createReview(any(), any());
    }
    @Test
    void createReview_validation_fail_rating_below_1() throws Exception {
        String body = String.format("{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":0,\"content\":\"good\"}", UUID.randomUUID(), UUID.randomUUID());
        mockMvc.perform(post("/v1/reviews").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verify(reviewService, never()).createReview(any(), any());
    }
    @Test
    void createReview_validation_fail_content_blank() throws Exception {
        String body = String.format("{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":3,\"content\":\"   \"}", UUID.randomUUID(), UUID.randomUUID());
        mockMvc.perform(post("/v1/reviews").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verify(reviewService, never()).createReview(any(), any());
    }
    @Test
    void createReview_validation_fail_content_null() throws Exception {
        String body = String.format("{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":3}", UUID.randomUUID(), UUID.randomUUID());
        mockMvc.perform(post("/v1/reviews").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verify(reviewService, never()).createReview(any(), any());
    }
    @Test
    void createReview_fail_user_not_found_propagates_404() throws Exception {
        when(reviewService.createReview(eq(CUSTOMER_ID), any()))
                .thenThrow(new CustomException(ErrorCode.USER_NOT_FOUND));
        String body = String.format("{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":4,\"content\":\"good\"}", UUID.randomUUID(), UUID.randomUUID());
        mockMvc.perform(post("/v1/reviews").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }
    @Test
    void createReview_fail_store_not_found_propagates_404() throws Exception {
        when(reviewService.createReview(eq(CUSTOMER_ID), any()))
                .thenThrow(new CustomException(ErrorCode.STORE_NOT_FOUND));
        String body = String.format("{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":4,\"content\":\"good\"}", UUID.randomUUID(), UUID.randomUUID());
        mockMvc.perform(post("/v1/reviews").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        verify(reviewService, times(1)).createReview(eq(CUSTOMER_ID), any());
    }
    @Test
    void createReview_fail_duplicate_review_propagates_409() throws Exception {
        when(reviewService.createReview(eq(CUSTOMER_ID), any()))
                .thenThrow(new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS));
        String body = String.format("{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":4,\"content\":\"good\"}", UUID.randomUUID(), UUID.randomUUID());
        mockMvc.perform(post("/v1/reviews").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        verify(reviewService, times(1)).createReview(eq(CUSTOMER_ID), any());
    }
    @Test
    void createReview_fail_unexpected_error_returns_500() throws Exception {
        when(reviewService.createReview(eq(CUSTOMER_ID), any()))
                .thenThrow(new RuntimeException("unexpected"));
        String body = String.format("{\"orderId\":\"%s\",\"storeId\":\"%s\",\"rating\":4,\"content\":\"good\"}", UUID.randomUUID(), UUID.randomUUID());
        mockMvc.perform(post("/v1/reviews").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isInternalServerError());
    }
    // PUT /v1/reviews/{reviewId}
    @Test
    void updateReview_success() throws Exception {
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateResponse response = ReviewUpdateResponse.builder()
                .reviewId(reviewId).rating(5).content("very good").updatedAt(LocalDateTime.now()).build();
        when(reviewService.updateReview(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), eq(reviewId), any()))
                .thenReturn(response);
        mockMvc.perform(put("/v1/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"content\":\"very good\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewId").value(reviewId.toString()))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.content").value("very good"));
        verify(reviewService, times(1)).updateReview(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), eq(reviewId), any());
    }
    @Test
    void updateReview_fail_not_owner_propagates_403() throws Exception {
        UUID reviewId = UUID.randomUUID();
        when(reviewService.updateReview(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), eq(reviewId), any()))
                .thenThrow(new CustomException(ErrorCode.REVIEW_FORBIDDEN));
        mockMvc.perform(put("/v1/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"content\":\"good\"}"))
                .andExpect(status().isForbidden());
        verify(reviewService, times(1)).updateReview(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), eq(reviewId), any());
    }
    @Test
    void updateReview_validation_fail_rating_below_1() throws Exception {
        UUID reviewId = UUID.randomUUID();
        mockMvc.perform(put("/v1/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":0,\"content\":\"bad\"}"))
                .andExpect(status().isBadRequest());
        verify(reviewService, never()).updateReview(any(), any(), any(), any());
    }
    @Test
    void updateReview_validation_fail_rating_above_5() throws Exception {
        UUID reviewId = UUID.randomUUID();
        mockMvc.perform(put("/v1/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":6,\"content\":\"too much\"}"))
                .andExpect(status().isBadRequest());
        verify(reviewService, never()).updateReview(any(), any(), any(), any());
    }
    @Test
    void updateReview_fail_review_not_found_propagates_404() throws Exception {
        UUID reviewId = UUID.randomUUID();
        when(reviewService.updateReview(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), eq(reviewId), any()))
                .thenThrow(new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        mockMvc.perform(put("/v1/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4,\"content\":\"good\"}"))
                .andExpect(status().isNotFound());
        verify(reviewService, times(1)).updateReview(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), eq(reviewId), any());
    }
    // GET /v1/reviews
    @Test
    void getReviews_customer_no_params_returns_own_reviews() throws Exception {
        ReviewResponse reviewResponse = ReviewResponse.builder()
                .userId(CUSTOMER_ID).rating(4).content("good").build();
        Page<ReviewResponse> page = new PageImpl<>(List.of(reviewResponse), PageRequest.of(0, 10), 1);
        when(reviewService.getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any(ReviewSearchRequest.class)))
                .thenReturn(page);
        mockMvc.perform(get("/v1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].userId").value(CUSTOMER_ID));
        verify(reviewService, times(1)).getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any(ReviewSearchRequest.class));
    }
    @Test
    void getReviews_no_params_empty_list() throws Exception {
        Page<ReviewResponse> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(reviewService.getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any(ReviewSearchRequest.class)))
                .thenReturn(emptyPage);
        mockMvc.perform(get("/v1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
        verify(reviewService, times(1)).getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any(ReviewSearchRequest.class));
    }
    @Test
    void getReviews_by_reviewId_success() throws Exception {
        UUID reviewId = UUID.randomUUID();
        UUID storeId  = UUID.randomUUID();
        ReviewResponse reviewResponse = ReviewResponse.builder()
                .reviewId(reviewId).userId(CUSTOMER_ID).storeId(storeId).rating(5).content("good").build();
        Page<ReviewResponse> page = new PageImpl<>(List.of(reviewResponse), PageRequest.of(0, 10), 1);
        when(reviewService.getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any(ReviewSearchRequest.class)))
                .thenReturn(page);
        mockMvc.perform(get("/v1/reviews").param("reviewId", reviewId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].reviewId").value(reviewId.toString()))
                .andExpect(jsonPath("$.data.content[0].rating").value(5));
        verify(reviewService, times(1)).getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any(ReviewSearchRequest.class));
    }
    @Test
    void getReviews_by_reviewId_forbidden_propagates_403() throws Exception {
        UUID reviewId = UUID.randomUUID();
        when(reviewService.getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any(ReviewSearchRequest.class)))
                .thenThrow(new CustomException(ErrorCode.REVIEW_FORBIDDEN));
        mockMvc.perform(get("/v1/reviews").param("reviewId", reviewId.toString()))
                .andExpect(status().isForbidden());
    }
    @Test
    void getReviews_by_reviewId_not_found_propagates_404() throws Exception {
        UUID reviewId = UUID.randomUUID();
        when(reviewService.getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any(ReviewSearchRequest.class)))
                .thenThrow(new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        mockMvc.perform(get("/v1/reviews").param("reviewId", reviewId.toString()))
                .andExpect(status().isNotFound());
    }
    @Test
    void getReviews_by_storeId_success() throws Exception {
        UUID storeId  = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        ReviewResponse reviewResponse = ReviewResponse.builder()
                .reviewId(reviewId).userId(CUSTOMER_ID).storeId(storeId).rating(3).content("ok").build();
        Page<ReviewResponse> page = new PageImpl<>(List.of(reviewResponse), PageRequest.of(0, 10), 1);
        when(reviewService.getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any(ReviewSearchRequest.class)))
                .thenReturn(page);
        mockMvc.perform(get("/v1/reviews").param("storeId", storeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.data.content[0].rating").value(3));
        verify(reviewService, times(1)).getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any(ReviewSearchRequest.class));
    }
    @Test
    void getReviews_by_storeId_empty() throws Exception {
        UUID storeId = UUID.randomUUID();
        Page<ReviewResponse> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(reviewService.getReviews(eq(CUSTOMER_ID), eq(CUSTOMER_ROLE), any(ReviewSearchRequest.class)))
                .thenReturn(emptyPage);
        mockMvc.perform(get("/v1/reviews").param("storeId", storeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }
    @Test
    void getReviews_invalid_page_size_returns_400() throws Exception {
        // size=20 is not in allowed set {10, 30, 50} -> @AssertTrue fails -> 400
        mockMvc.perform(get("/v1/reviews").param("size", "20"))
                .andExpect(status().isBadRequest());
        verify(reviewService, never()).getReviews(any(), any(), any());
    }
    // DELETE /v1/reviews/{reviewId}
    @Test
    void deleteReview_success() throws Exception {
        UUID reviewId = UUID.randomUUID();
        doNothing().when(reviewService).deleteReview(eq(reviewId), eq(CUSTOMER_ID), eq(CUSTOMER_ROLE));
        mockMvc.perform(delete("/v1/reviews/" + reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("리뷰 삭제 성공"));
        verify(reviewService, times(1)).deleteReview(eq(reviewId), eq(CUSTOMER_ID), eq(CUSTOMER_ROLE));
    }
    @Test
    void deleteReview_fail_not_owner_propagates_403() throws Exception {
        UUID reviewId = UUID.randomUUID();
        doThrow(new CustomException(ErrorCode.REVIEW_FORBIDDEN))
                .when(reviewService).deleteReview(eq(reviewId), eq(CUSTOMER_ID), eq(CUSTOMER_ROLE));
        mockMvc.perform(delete("/v1/reviews/" + reviewId))
                .andExpect(status().isForbidden());
        verify(reviewService, times(1)).deleteReview(eq(reviewId), eq(CUSTOMER_ID), eq(CUSTOMER_ROLE));
    }
    @Test
    void deleteReview_fail_not_found_propagates_404() throws Exception {
        UUID reviewId = UUID.randomUUID();
        doThrow(new CustomException(ErrorCode.REVIEW_NOT_FOUND))
                .when(reviewService).deleteReview(eq(reviewId), eq(CUSTOMER_ID), eq(CUSTOMER_ROLE));
        mockMvc.perform(delete("/v1/reviews/" + reviewId))
                .andExpect(status().isNotFound());
        verify(reviewService, times(1)).deleteReview(eq(reviewId), eq(CUSTOMER_ID), eq(CUSTOMER_ROLE));
    }
}