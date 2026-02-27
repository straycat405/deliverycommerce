package com.babjo.deliverycommerce.global.common.dto;

/**
 * 공통 API 응답 래퍼 클래스
 *
 * 성공 - 200
 * return ResponseEntity.ok(ApiResponse.success(responseDto));
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

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T> {

    private int status;
    private String code;
    private String message;
    private T data;

    // 성공 응답 (200 OK)
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> res = new ApiResponse<>();
        res.status = 200;
        res.code = "SUCCESS";
        res.message = "요청 성공";
        res.data = data;
        return res;
    }

    // 상태코드 커스텀 성공 응답 (201 Created 등)
    public static <T> ApiResponse<T> success(HttpStatus httpStatus, String message, T data) {
        ApiResponse<T> res = new ApiResponse<>();
        res.status = httpStatus.value();
        res.code = "SUCCESS";
        res.message = message;
        res.data = data;
        return res;
    }

    // 실패 응답
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        ApiResponse<T> res = new ApiResponse<>();
        res.status = errorCode.getStatus().value();
        res.code = errorCode.getCode();
        res.message = errorCode.getMessage();
        return res;
    }

    // 실패 응답 - message 동적 교체용
    // @Valid 검증 실패 사유 등을 메세지로 전달하고 싶을때 message를 함께 담아서 보내줍니다.
    // 오버로드 하지 않은 실패 응답은 ErrorCode상의 메시지만 리턴함
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        ApiResponse<T> res = new ApiResponse<>();
        res.status = errorCode.getStatus().value();
        res.code = errorCode.getCode();
        res.message = message; // ErrorCode 기본 메시지 대신 동적 메시지 사용
        return res;
    }
}
