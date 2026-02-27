package com.babjo.deliverycommerce.global.jwt;

import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Slf4j(topic = "JwtUtil")
@Component
public class JwtUtil {

    // Authorization Header KEY
    public static final String AUTHORIZATION_HEADER = "Authorization";
    // JWT payload에 담을 권한 (role) 클레임 키
    public static final String AUTHORIZATION_KEY = "auth";
    // Bearer 토큰 식별자 (헤더값 파싱 시 제거)
    public static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-expiration}")
    private long jwtAccessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long jwtRefreshExpiration;

    private SecretKey key;

    // 빈 초기화 후 자동 실행 - secretKey 주입 완료 후 Key 객체 생성
    // @PostConstruct -> 의존성 주입 완료 후 딱 한번 실행
    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(jwtSecret);
        key = Keys.hmacShaKeyFor(bytes);
    }

    /**
     * Access Token 생성
     * payload에 userId, username, role 담아서 서명
      * @param userId
     * @param username
     * @param role ( CUSTOMER / OWNER / MANAGER / MASTER )
     */
    public String createAccessToken(Long userId, String username, String role) {
        Date now = new Date();
        return BEARER_PREFIX + Jwts.builder()
                .subject(String.valueOf(userId)) // sub 클레임 = userId
                .claim("username", username)
                .claim(AUTHORIZATION_KEY, role) // auth 클레임 = role
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtAccessExpiration))
                .signWith(key)
                .compact();
    }

    /**
     * Refresh Token 생성
     * Access Token 재발급 용도로만 사용 (payload 최소화)
     * @param userId 사용자 PK
     */
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtRefreshExpiration))
                .signWith(key)
                .compact();
    }

    /**
     * Authorization 헤더값에서 순수 토큰값 추출
     */
    public String subStringToken(String token) {
        if (StringUtils.hasText(token) && token.startsWith(BEARER_PREFIX)) {
            return token.substring(BEARER_PREFIX.length());
        }
        throw new CustomException(ErrorCode.INVALID_TOKEN);
    }


    /**
     * 토큰에서 Claims(payload 전체) 추출
     * userId, username, role
     */
    public Claims getUserInfoFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload(); // getBody() → getPayload()
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.", e);
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (SecurityException | MalformedJwtException e) {
            log.error("유효하지 않은 JWT 서명입니다.", e);
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (UnsupportedJwtException | IllegalArgumentException e) {
            log.error("지원하지 않는 JWT 토큰입니다.", e);
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

    }

    /**
     * 토큰 남은 만료시간 반환 (ms단위)
     * 로그아웃 시 Redis Blaclist TTL 계산에 사용
     */
    public long getRemainExpiration(String token) {
        Claims claims = getUserInfoFromToken(token);
        return claims.getExpiration().getTime() - new Date().getTime();
    }

    /**
     * 토큰 유효성 검사
     * 만료 / 위변조 / 미지원 형식을 구분해서 CustomException으로 던짐
     * GlobalExceptionHandler가 자동으로 잡아서 응답 처리
     * 재발급 등 만료 여부를 별도로 구분해야 하는 케이스에서 사용
     * 일반 인가 필터는 getUserInfoFromToken() 사용
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            // 만료 - 재발급 로직에서 구분 처리
            log.error("만료된 JWT 토큰입니다.", e);
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (SecurityException | MalformedJwtException e) {
            // 서명 불일치 또는 토큰 구조 손상
            log.error("유효하지 않은 JWT 서명입니다.", e);
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (UnsupportedJwtException | IllegalArgumentException e) {
            // 지원하지 않는 형식 또는 빈 값
            log.error("지원하지 않는 JWT 토큰입니다.", e);
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

}
