package com.babjo.deliverycommerce.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.UnknownFormatConversionException;

@Slf4j(topic = "JwtUtil")
@Component
public class JwtUtil {

    // Header KEY
    public static final String AUTHORIZATION_HEADER = "Authorization";
    // 사용자 권한 값의 KEY
    public static final String AUTHORIZATION_KEY = "auth";
    // Token 식별자
    public static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-expiration}")
    private long jwtAccessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long jwtRefreshExpiration;

    private Key key;

    @PostConstruct // 빈 초기화 후 자동 실행 - secretKey 주입 완료 후 Key 객체 생성
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(jwtSecret);
        key = Keys.hmacShaKeyFor(bytes);
    }

    // Access Token 생성
    public String createAccessToken(Long userId, String username, String role) {
        Date now = new Date();
        return BEARER_PREFIX + Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim(AUTHORIZATION_KEY, role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtAccessExpiration))
                .signWith(key)
                .compact();
    }

    // Refresh Token 생성
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtRefreshExpiration))
                .signWith(key)
                .compact();
    }

    // 토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith((SecretKey) key).build().parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("유효하지 않은 JWT 서명입니다.", e);
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.", e);
        } catch (UnsupportedJwtException e) {
            log.error("지원하지 않는 JWT 토큰입니다.", e);
        } catch (IllegalArgumentException e) {
            log.error("JWT 클레임이 비어있습니다.", e);
        }
        return false;
    }

    // Claims 추출
    public Claims getUserInfoFromToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();                          // getBody() → getPayload()
    }

    // 남은 만료시간 추출
    public long getRemainExpiration(String token) {
        Claims claims = getUserInfoFromToken(token);
        return claims.getExpiration().getTime() - new Date().getTime();
    }

}
