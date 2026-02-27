package com.babjo.deliverycommerce.global.jwt;

import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.redis.RedisKeys;
import com.babjo.deliverycommerce.global.redis.RedisUtil;
import com.babjo.deliverycommerce.global.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j(topic = "JwtAuthorizationFilter")
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    // UserDetailsServiceImpl 제거 — DB 조회 안 하므로 불필요
    public JwtAuthorizationFilter(JwtUtil jwtUtil, RedisUtil redisUtil) {
        this.jwtUtil = jwtUtil;
        this.redisUtil = redisUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain)
            throws ServletException, IOException {

        String header = req.getHeader(JwtUtil.AUTHORIZATION_HEADER);

        // 헤더가 없거나 Bearer로 시작하지 않으면 토큰 없는 요청으로 간주, 그냥 통과
        // → permitAll() 엔드포인트는 그대로 통과, 인증 필요 엔드포인트는 이후 Security가 401 처리
        if (!StringUtils.hasText(header) || !header.startsWith(JwtUtil.BEARER_PREFIX)) {
            filterChain.doFilter(req, res);
            return;
        }

        try {
            String tokenValue = jwtUtil.subStringToken(header);

            // Blacklist 체크 - 로그아웃된 토큰 차단
            if (redisUtil.hasKey(RedisKeys.blacklistKey(tokenValue))) {
                log.warn("[JWT] 블랙리스트 토큰 차단");
                filterChain.doFilter(req, res);
                return;
            }

            // 내부에서 파싱 + 서명 검증 동시 수행
            // 만료 / 위변조 시 CustomException 발생
            Claims claims = jwtUtil.getUserInfoFromToken(tokenValue);

            // DB 조회 없이 Claims에서 직접 인증 객체 생성
            setAuthentication(claims);

        } catch (CustomException e) {
            // 만료 / 위변조 등 → SecurityContext 비워둔 채 진행
            // EntryPoint가 401 응답 처리
            log.error("[JWT] code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("[JWT] 알 수 없는 오류", e);
        }

        filterChain.doFilter(req, res);
    }

    // Claims → SecurityContext 세팅
    private void setAuthentication(Claims claims) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(createAuthentication(claims));
        SecurityContextHolder.setContext(context);
    }

    // DB 조회 없이 Claims에서 직접 Authentication 생성
    private Authentication createAuthentication(Claims claims) {
        Long userId = Long.parseLong(claims.getSubject());      // subject = userId
        String username = claims.get("username", String.class); // JWT payload의 username
        String role = claims.get("role", String.class);         // JWT payload의 role

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role)
        );

        // 컨트롤러에서 @AuthenticationPrincipal UserPrincipal로 꺼낼 수 있다
        UserPrincipal principal = new UserPrincipal(userId, username, role);

        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }
}