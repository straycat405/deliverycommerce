package com.babjo.deliverycommerce.global.redis;

/**
 *   Redis 키 구조:
 *     refresh:{userId}       → refreshToken 문자열  (TTL: 7일)
 *     blacklist:{token}      → "blacklisted"         (TTL: 잔여 만료시간)
 *     user:{userId}:auth     → UserAuthCache JSON    (TTL: 없음, 영구지속)
 *
 *   UserAuthCache JSON 예시:
 *     {
 *       "status": "ACTIVE",
 *       "role": "ROLE_CUSTOMER",
 *       "authVersion": 1
 *     }
 */

import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAuthCacheManager {

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    /**
     * 사용자 인증 상태 저장 (영구 저장)
     * 호출 시점: 로그인, 권한 변경, 삭제
     */
    public void save(Long userId, UserAuthCache cache) {
        try {
            String json = objectMapper.writeValueAsString(cache);
            redisUtil.set(RedisKeys.userAuthKey(userId), json);
        } catch (JsonProcessingException e) {
            log.error("[UserAuthCache] 직렬화 실패 - userId: {}", userId);
            throw new CustomException(ErrorCode.REDIS_OPERATION_FAILED);
        }
    }

    /**
     * 사용자 인증 상태 저장 (비영구 저장) - TTL포함
     * 호출 시점: 로그인, 권한 변경, 삭제
     */
    public void saveWithTtl(Long userId, UserAuthCache cache, long duration, TimeUnit timeUnit) {
        try {
            String json = objectMapper.writeValueAsString(cache);
            redisUtil.set(RedisKeys.userAuthKey(userId), json, duration, timeUnit);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.REDIS_OPERATION_FAILED);
        }
    }

    /**
     * 사용자 인증 상태 조회
     * 호출 시점: JwtAuthorizationFilter 매 요청마다
     * 반환값이 null이면 Redis miss → 상위에서 DB 조회 후 save() 호출
     */
    public UserAuthCache get(Long userId) {
        String json = redisUtil.get(RedisKeys.userAuthKey(userId));
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, UserAuthCache.class);
        } catch (JsonProcessingException e) {
            log.error("[UserAuthCache] 역직렬화 실패 - userId: {}", userId);
            throw new CustomException(ErrorCode.REDIS_OPERATION_FAILED);
        }
    }

    /**
     * 사용자 인증 상태 삭제
     * 호출 시점: 필요 시 (캐시 무효화)
     */
    public void delete(Long userId) {
        redisUtil.delete(RedisKeys.userAuthKey(userId));
    }
}
