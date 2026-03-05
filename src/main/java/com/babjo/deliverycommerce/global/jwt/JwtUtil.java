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
import java.util.Base64;
import java.util.Date;

@Slf4j(topic = "JwtUtil")
@Component
public class JwtUtil {

    // Authorization Header KEY
    public static final String AUTHORIZATION_HEADER = "Authorization";
    // JWT payload에 담을 권한 (role) 클레임 키
    public static final String AUTHORIZATION_KEY = "roles";
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
     *
     * @param userId
     * @param username
     * @param role     ( CUSTOMER / OWNER / MANAGER / MASTER )
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
     *
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
     * Claims 추출 — 필터에서 사용
     */
    public Claims getUserInfoFromToken(String token) {
        return parseToken(token);
    }

    /**
     * 유효성 검사 — 재발급 등 별도 분기 필요 시 사용
     */
    public boolean validateToken(String token) {
        parseToken(token); // 예외 안 나면 유효한 것
        return true;
    }

    /**
     * Access Token 남은 만료시간 반환 (ms)
     */
    public long getRemainExpiration(String token) {
        return parseToken(token).getExpiration().getTime() - new Date().getTime();
    }

    /**
     * Refresh Token 만료시간 반환 (ms)
     * Controller에서 Cookie maxAge 세팅 시 사용
     * maxAge는 초 단위이므로 / 1000 변환 필요
     */
    public long getRefreshExpiration() {
        return jwtRefreshExpiration;
    }

    /**
     * 공통 파싱 메서드 — 내부 전용
     * 만료 / 위변조 / 미지원 모두 CustomException으로 변환
     */
    private Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.", e);
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (SecurityException | MalformedJwtException e) {
            log.error("유효하지 않은 JWT 서명입니다.", e);
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (UnsupportedJwtException | IllegalArgumentException e) {
            log.error("지원하지 않는 JWT 토큰입니다.", e);
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

}
