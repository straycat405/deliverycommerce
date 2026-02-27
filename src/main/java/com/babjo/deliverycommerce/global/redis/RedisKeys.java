package com.babjo.deliverycommerce.global.redis;

public final class RedisKeys {

    private RedisKeys() {} // 인스턴스화 방지

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