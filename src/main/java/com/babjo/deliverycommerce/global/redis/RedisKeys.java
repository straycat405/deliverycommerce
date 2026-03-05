package com.babjo.deliverycommerce.global.redis;

/**
 *   Redis 키 구조
 *
 *   user:{userId}:auth     → UserAuthCache JSON
 *   refresh:{userId}       → Refresh Token 문자열
 *   blacklist:{token}      → "blacklisted" 문자열
 */

public final class RedisKeys {
    private RedisKeys() {
    } // 인스턴스화 방지

    // user:{userId}:auth → UserAuthCache JSON
    public static final String USER_AUTH_PREFIX = "user:";
    public static final String USER_AUTH_SUFFIX = ":auth";

    public static String userAuthKey(Long userId) {
        return USER_AUTH_PREFIX + userId + USER_AUTH_SUFFIX;
        // 결과: "user:1:auth"
    }

    // 리프레시 토큰 저장용 prefix
    // 사용: "refresh:" + userId → "refresh:1"
    public static final String REFRESH_TOKEN_PREFIX = "refresh:";

    // 로그아웃된 액세스 토큰 블랙리스트 prefix
    // 사용: "blacklist:" + accessToken → "blacklist:eyJhbGci..."
    public static final String BLACKLIST_PREFIX = "blacklist:";

    // 편의 메서드 — 직접 조합해서 쓰기
    public static String refreshKey(Long userId) {
        return REFRESH_TOKEN_PREFIX + userId;
    }

    public static String blacklistKey(String token) {
        return BLACKLIST_PREFIX + token;
    }
}