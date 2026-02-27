package com.babjo.deliverycommerce.global.jwt;

import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.security.UserDetailsServiceImpl;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j(topic = "JwtAuthorizationFilter")
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthorizationFilter(JwtUtil jwtUtil, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
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

            // jwtUtil.getUserInfoFromToken() 내부에서 파싱 + 서명 검증을 동시에 수행
            // 만료 / 위변조 시 여기서 바로 CustomException 발생

            Claims info = jwtUtil.getUserInfoFromToken(tokenValue);

            // subject에 userId를 넣었으므로 getSubject() 사용
            setAuthentication(info.getSubject());

        } catch (CustomException e) {
            // 만료 / 위변조 등 → SecurityContext 비워둔 채 진행
            // EntryPoint가 401 응답 처리
            log.error("[JWT 오류] code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
            filterChain.doFilter(req, res);
            return;
        } catch (Exception e) {
            log.error("[JWT 오류] 알 수 없는 오류", e);
            filterChain.doFilter(req, res);
            return;
        }

        filterChain.doFilter(req, res);
    }

    public void setAuthentication(String userId) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = createAuthentication(userId);
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private Authentication createAuthentication(String userId) {
        // String -> Long변환 후 userId 전용 메서드 호출
        UserDetails userDetails = userDetailsService.loadUserByUserId(Long.parseLong(userId));
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}