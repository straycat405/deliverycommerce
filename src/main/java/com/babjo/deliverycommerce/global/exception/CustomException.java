package com.babjo.deliverycommerce.global.exception;

/**
 * 비즈니스 예외 클래스
 *
 * 서비스 로직에서 의도적으로 발생시키는 예외를 표현합니다.
 * GlobalExceptionHandler가 자동으로 잡아서 CommonResponse 형식으로 응답을 처리합니다.
 *
 * ── 사용법 ───────────────────────────────────────────────────────
 *
 *   throw new CustomException(ErrorCode.USER_NOT_FOUND);
 *
 * ── 주의사항 ──────────────────────────────────────────────────────
 *
 *   1. try-catch로 직접 잡아서 ResponseEntity 반환하지 말 것
 *      → 핸들러를 거치지 않으면 응답 형식이 깨집니다.
 *
 *   2. ErrorCode는 ErrorCode.java에 정의된 값만 사용할 것
 *      → 새 에러코드 필요 시 ErrorCode.java 주석 참고 후 추가
 *
 *   3. Controller / Service 어디서든 던질 수 있다.
 *      Repository 계층에서는 orElseThrow() 람다 안에서 사용하면 된다.
 *
 *      ex) userRepository.findById(userId)
 *              .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
 */

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException{

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode){
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
