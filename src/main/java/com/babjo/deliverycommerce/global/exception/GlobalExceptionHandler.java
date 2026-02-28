package com.babjo.deliverycommerce.global.exception;

import com.babjo.deliverycommerce.global.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리 핸들러
 *
 *
 *   예외는 아래 두 가지 방식으로만 던져주세요. 핸들러가 자동으로 잡아서 응답 처리합니다.
 *
 *   [비즈니스 예외]
 *     throw new CustomException(ErrorCode.USER_NOT_FOUND);
 *
 *   [유효성 검증 실패]
 *     DTO 필드에 @NotBlank, @Size 등 달아두면 자동 처리됨
 *     별도 throw 불필요
 *
 *   새 예외 타입 추가가 필요하면
 *   Git 관리자(부조장)에게 요청할 것
 *
 *   주의
 *   try-catch로 직접 잡아서 ResponseEntity 반환하지 말 것
 *   핸들러를 거치지 않으면 응답 형식이 깨짐
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 직접 던지는 예외
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustomException(CustomException e) {
        log.warn("[CustomException] code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        // 내부 status와 HTTP status를 한 번에 해결
        return ApiResponse.error(e.getErrorCode());
    }

    // @Valid 유효성 검사 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.INVALID_INPUT.getMessage());

        log.warn("[ValidationException] message={}", message);

        // ResponseEntity.status().body()를 생략하고 바로 반환
        return ApiResponse.error(ErrorCode.INVALID_INPUT, message);
    }
    // 나머지 예상 못한 예외상황
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> unhandledException(Exception e) {
        log.error("[UnhandledException]", e);
        // 여기서도 ApiResponse.error()만 사용
        return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
