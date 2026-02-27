package com.babjo.deliverycommerce.global.redis;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 값 저장 (TTL 포함)
     */
    public void set(String key, String value, long duration, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, duration, timeUnit);
    }

    /**
     * 값 조회
     * ex) get("refresh:1") - userId
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 키 삭제
     * ex) 로그아웃 시 refresh:{userId} 삭제
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 키 존재 여부 확인
     * ex) blacklist 토큰 체크
     */
    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 남은 TTL 조회 (단위 : 초)
     * ex) Access Token 블랙리스트 저장 시 남은 만료 시간 계산용
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
}
