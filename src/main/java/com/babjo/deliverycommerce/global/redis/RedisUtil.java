package com.babjo.deliverycommerce.global.redis;


import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 값 저장 (TTL 포함)
     */
    public void set(String key, String value, long duration, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, duration, timeUnit);
        } catch (DataAccessException e) {
            log.error("[Redis] set 실패 - key: {}, error: {}", key, e.getMessage());
            throw new CustomException(ErrorCode.REDIS_OPERATION_FAILED);
        }
    }

    /**
     * 값 조회
     * ex) get("refresh:1") - userId
     */
    public String get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (DataAccessException e) {
            throw new CustomException(ErrorCode.REDIS_OPERATION_FAILED);
        }
    }

    /**
     * 키 삭제
     * ex) 로그아웃 시 refresh:{userId} 삭제
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (DataAccessException e) {
            throw new CustomException(ErrorCode.REDIS_OPERATION_FAILED);
        }
    }

    /**
     * 키 존재 여부 확인
     * ex) blacklist 토큰 체크
     * Redis 연결 장애등으로 null 반환 -> NPE 가능성 주의
     */
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (DataAccessException e) {
            throw new CustomException(ErrorCode.REDIS_OPERATION_FAILED);
        }
    }

    /**
     * 남은 TTL 조회 (단위 : 초)
     * ex) Access Token 블랙리스트 저장 시 남은 만료 시간 계산용
     * redisTemplate의 getExpire()는 상황에 따라 음수를 반환
     * -1 : 키는 존재하지만 TTL이 없음 (만료 없이 저장된 경우)
     * -2 : 키가 존재하지 않음
     * 따라서 음수 반환값에 대한 처리가 필요함
     */
    public long getExpire(String key) {
        try {
            long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return Math.max(ttl, 0); // 음수면 0 반환
        } catch (DataAccessException e) {
            throw new CustomException(ErrorCode.REDIS_OPERATION_FAILED);
        }
    }
}
