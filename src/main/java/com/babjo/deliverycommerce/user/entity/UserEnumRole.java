package com.babjo.deliverycommerce.user.entity;

import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;

public enum UserEnumRole {

    CUSTOMER(Authority.CUSTOMER),
    OWNER(Authority.OWNER),
    MANAGER(Authority.MANAGER),
    MASTER(Authority.MASTER);

    private final String authority;

    // 생성자
    UserEnumRole(String authority) {
        this.authority = authority;
    }

    public static class Authority {

        public static final String CUSTOMER = "ROLE_CUSTOMER";
        public static final String OWNER = "ROLE_OWNER";
        public static final String MANAGER = "ROLE_MANAGER";
        public static final String MASTER = "ROLE_MASTER";
    }
    /**
     * 문자열 → UserEnumRole 변환
     * 회원가입시 String값 CUSTOMER / OWNER만 허용
     * 잘못된 값 또는 MANAGER / MASTER 입력 시 INVALID_ROLE 예외
     */
    public static UserEnumRole ofSignup(String roleName) {
        try {
            UserEnumRole role = UserEnumRole.valueOf(roleName.toUpperCase());
            if (role == MANAGER || role == MASTER) {
                throw new CustomException(ErrorCode.INVALID_ROLE);
            }
            return role;
        } catch (IllegalArgumentException e) {
            // valueOf() 실패 — 정의되지 않은 값
            throw new CustomException(ErrorCode.INVALID_ROLE);
        }
    }

    /**
     * 문자열 → UserEnumRole 변환 (전체 허용, 관리자용)
     * 권한 수정 API 등에서 사용
     */
    public static UserEnumRole of(String roleName) {
        try {
            return UserEnumRole.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_ROLE);
        }
    }

    // Getter
    public String getAuthority() {
        return authority;
    }
}
