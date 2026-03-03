package com.babjo.deliverycommerce.global.common.dto;

/**
 * 공통 API 응답 래퍼 클래스
 *
 * 성공 - 200
 * return ApiResponse.ok("로그인 성공", loginResponseDto);
 *
 * 실패
 * -> GlobalExceptionHandler가 자동 처리하므로 직접 호출하지 않습니다.
 * → 에러는 throw new CustomException(ErrorCode.XXX) 로만 던질 것
 *
 * 주의
 * ResponseEntity<ApiResponse<?>> 형태로 반환 타입 맞출 것
 */

import com.babjo.deliverycommerce.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T> {

    private int status;
    private String code;
    private String message;
    private T data;

    /**
     * 내부 생성자 - static factory method 사용 강제
     */
    private ApiResponse(int status, String code, String message, T data) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
    }

// ── 성공 응답 (HTTP 200 OK) ──────────────────────────────────

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", "요청에 성공하였습니다.", data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", message, data));
    }

    // 데이터 없이 성공 메시지만 보낼 때 (ex. 로그아웃 성공)
    public static ResponseEntity<ApiResponse<?>> ok(String message) {
        return ResponseEntity.ok(new ApiResponse<>(200, "SUCCESS", message, null));
    }

    // ── 생성 응답 (HTTP 201 Created) ──────────────────────────────

    public static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(201, "SUCCESS", message, data));
    }

    // ── 삭제/수정 후 응답 (HTTP 204 No Content) ───────────────────
    // Body가 아예 없는 것이 표준이나, 공통 규격 유지를 위해 200 혹은 204 선택 사용
    public static ResponseEntity<ApiResponse<?>> noContent() {
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    // ── 실패 응답 (GlobalExceptionHandler용) ────────────────────

    public static ResponseEntity<ApiResponse<?>> error(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ApiResponse<>(errorCode.getStatus().value(), errorCode.getCode(), errorCode.getMessage(), null));
    }

    public static ResponseEntity<ApiResponse<?>> error(ErrorCode errorCode, String message) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ApiResponse<>(errorCode.getStatus().value(), errorCode.getCode(), message, null));
    }

    // ── Filter용 (ResponseEntity 없이 객체만 반환) ────────────────────
    public static ApiResponse<?> errorBody(ErrorCode errorCode) {
        return new ApiResponse<>(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );
    }
}
