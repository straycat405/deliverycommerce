package com.babjo.deliverycommerce.global.exception;

import com.babjo.deliverycommerce.global.common.dto.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리 핸들러
 *
 *   [비즈니스 예외]
 *     throw new CustomException(ErrorCode.USER_NOT_FOUND);
 *
 *   [유효성 검증 실패]
 *     DTO 필드에 @NotBlank, @Size 등으로 유효성 검증
 *     별도 throw 불필요
 *
 *   새 예외 타입 추가가 필요하면
 *   Git 관리자에게 요청할 것
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
    public ResponseEntity<CommonResponse<?>> handleCustomException(CustomException e) {
        log.warn("[CustomException] code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        // 내부 status와 HTTP status를 한 번에 해결
        return CommonResponse.error(e.getErrorCode());
    }

    // @Valid 유효성 검사 실패한 경우 던지는 예외
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.INVALID_INPUT.getMessage());

        log.warn("[ValidationException] message={}", message);

        // ResponseEntity.status().body()를 생략하고 바로 반환
        return CommonResponse.error(ErrorCode.INVALID_INPUT, message);
    }

    // @PreAuthorize 권한 거부시 던지는 예외
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<CommonResponse<?>> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        log.warn("[AuthorizationDeniedException] message={}", e.getMessage());
        return CommonResponse.error(ErrorCode.FORBIDDEN);
    }

    // Request JSON 필드 비어있는 경우 던지는 예외
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse<?>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("[HttpMessageNotReadableException] message={}", e.getMessage());
        return CommonResponse.error(ErrorCode.INVALID_INPUT);
    }

    // 기타 접근 거부 (Filter레벨)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CommonResponse<?>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("[AccessDeniedException] message={}", e.getMessage());
        return CommonResponse.error(ErrorCode.FORBIDDEN);
    }

    // 나머지 예상 못한 예외상황
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<?>> unhandledException(Exception e) {
        log.error("[UnhandledException]", e);
        // 여기서도 CommonResponse.error()만 사용
        return CommonResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
