package com.babjo.deliverycommerce.global.exception;

/**
 *  전역 에러 코드 Enum
 *
 *  사용법
 *  EX)throw new CustomException(ErrorCode.USER_NOT_FOUND);
 *
 *  새 에러코드 추가할때
 *  1. 본인 도메인 영역 주석 아래에 추가
 *  2. 형식 : 에러명(HttpStatus.상태코드, "에러코드", "메시지 내용")
 *  ex) ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "존재하지 않는 주문입니다.")
 *  3. develop 브랜치에 PR 날려서 충돌 방지할 것 (동시 수정 지양)
 *  4. 에러코드 문자열( 2nd Parameter)는 EUNM 이름과 동일하게
 */

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── Auth / Token ──────────────────────────────────
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN", "만료된 토큰입니다. 다시 로그인해 주세요."),
    TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "TOKEN_BLACKLISTED", "이미 로그아웃된 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다."),
    TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "TOKEN_MISMATCH", "토큰 정보가 일치하지 않습니다. 다시 로그인해주세요."),

    // ── User ──────────────────────────────────────────
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 사용자입니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "DUPLICATE_USERNAME", "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "비밀번호가 일치하지 않습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    ALREADY_DELETED(HttpStatus.CONFLICT, "ALREADY_DELETED", "이미 탈퇴한 사용자입니다."),
    WITHDRAWN_USER(HttpStatus.BAD_REQUEST, "WITHDRAWN_USER", "탈퇴한 계정입니다."),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "INVALID_ROLE", "유효하지 않은 권한 값입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다."),
    CANNOT_DELETE_SELF(HttpStatus.FORBIDDEN,"CANNOT_DELETE_SELF","본인 계정은 삭제할 수 없습니다."),
    CANNOT_UPDATE_ROLE_SELF(HttpStatus.FORBIDDEN,"CANNOT_UPDATE_ROLE_SELF","본인 권한은 변경할 수 없습니다."),

    // ── Common ────────────────────────────────────────
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "입력값이 유효하지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "INVALID_PAGE_SIZE", "페이지 크기는 10, 30, 50만 가능합니다."),
    INVALID_SORT_BY(HttpStatus.BAD_REQUEST, "INVALID_SORT_BY", "정렬 기준이 올바르지 않습니다."),

    // ── Infrastructure  ─────────────────────
    REDIS_OPERATION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "REDIS_OPERATION_FAILED", "인증 처리 중 오류가 발생했습니다."),

    // ── Store / Product / Order / Payment / Review ────
    // 각 도메인 담당자가 PR로 추가

    // ── Product  ──────────────────────────────────────────
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "존재하지 않는 상품입니다."),
    PRODUCT_DELETED(HttpStatus.GONE, "PRODUCT_DELETED", "삭제된 상품입니다."),
  
    // ── Store ─────────────────────────────────────────
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND", "존재하지 않는 가게입니다."),
    STORE_FORBIDDEN(HttpStatus.FORBIDDEN, "STORE_FORBIDDEN", "해당 가게에 대한 권한이 없습니다."),


    // ── Cart ─────────────────────────────────────────
    /*수정/삭제하려는 CartItem이 없을때 */
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND", "존재하지 않는 장바구니 항목입니다."),

    CART_FORBIDDEN(HttpStatus.FORBIDDEN, "CART_FORBIDDEN", "해당 장바구니에 대한 권한이 없습니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY", "수량은 1 이상이어야 합니다."),

    /*한 장바구니에 다른 Store 상품을 담으려 할 때*/
    CART_STORE_MISMATCH(HttpStatus.BAD_REQUEST, "CART_STORE_MISMATCH", "다른 가게의 상품은 같은 장바구니에 담을 수 없습니다."),

    // ── Review ────────────────────────────────────────
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "존재하지 않는 리뷰입니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS", "이미 리뷰를 작성했습니다."),
    REVIEW_FORBIDDEN(HttpStatus.FORBIDDEN, "REVIEW_FORBIDDEN", "해당 리뷰에 대한 권한이 없습니다."),

    // ── Payment ───────────────────────────────────────
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "존재하지 않는 결제입니다."),
    PAYMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "PAYMENT_ALREADY_EXISTS", "해당 주문에 이미 결제가 존재합니다."),
    PAYMENT_INVALID_STATUS(HttpStatus.BAD_REQUEST, "PAYMENT_INVALID_STATUS", "현재 결제 상태에서는 해당 작업을 수행할 수 없습니다."),
    PAYMENT_ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "PAYMENT_ALREADY_CANCELED", "이미 취소된 결제입니다."),
    PAYMENT_CANCEL_TIME_EXPIRED(HttpStatus.BAD_REQUEST, "PAYMENT_CANCEL_TIME_EXPIRED", "결제 취소 가능 시간(5분)이 초과되었습니다."),
    PAYMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "PAYMENT_FORBIDDEN", "해당 결제에 대한 권한이 없습니다."),

    // ── AiLog  ──────────────────────────────────────────
    AI_GENERATION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "AI_GENERATION_FAILED", "AI 설명 생성에 실패했습니다. 잠시 후 다시 시도해주세요."),
    AI_LOG_FORBIDDEN(HttpStatus.FORBIDDEN, "AI_LOG_FORBIDDEN", "AI 로그 조회 권한이 없습니다."),
    AI_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "AI_LOG_NOT_FOUND", "존재하지 않는 AI 로그입니다."),

    // ── Order ─────────────────────────────────────────
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "존재하지 않는 주문입니다."),
    ORDER_ALREADY_DELETED(HttpStatus.GONE, "ORDER_ALREADY_DELETED", "삭제된 주문 내역입니다."),
    NOT_ORDER_USER(HttpStatus.FORBIDDEN, "NOT_ORDER_USER", "본인의 주문에 대해서만 처리 가능합니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "INVALID_ORDER_STATUS", "현재 주문 상태에서는 수행할 수 없는 작업입니다."),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ORDER_ACCESS_DENIED", "주문에 대한 접근 권한이 없습니다."),
    INVALID_COOKING_TIME(HttpStatus.BAD_REQUEST, "INVALID_COOKING_TIME", "올바른 조리 시간을 입력해주세요. ( 1분 이상 )"),
    PRICE_MISMATCH(HttpStatus.BAD_REQUEST, "PRICE_MISMATCH", "상품 가격 정보가 일치하지 않습니다."),
    ORDER_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "ORDER_NOT_COMPLETED", "픽업이 완료된 주문에만 리뷰를 작성할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
    }