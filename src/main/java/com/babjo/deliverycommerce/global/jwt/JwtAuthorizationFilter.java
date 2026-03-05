package com.babjo.deliverycommerce.global.jwt;

import com.babjo.deliverycommerce.global.common.dto.CommonResponse;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.babjo.deliverycommerce.global.redis.RedisKeys;
import com.babjo.deliverycommerce.global.redis.RedisUtil;
import com.babjo.deliverycommerce.global.redis.UserAuthCache;
import com.babjo.deliverycommerce.global.redis.UserAuthCacheManager;
import com.babjo.deliverycommerce.global.security.UserPrincipal;
import com.babjo.deliverycommerce.user.entity.User;
import com.babjo.deliverycommerce.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;
    private final UserAuthCacheManager userAuthCacheManager;
    private final UserRepository userRepository;

    public JwtAuthorizationFilter(JwtUtil jwtUtil, RedisUtil redisUtil, ObjectMapper objectMapper,
                                  UserAuthCacheManager userAuthCacheManager, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.redisUtil = redisUtil;
        this.objectMapper = objectMapper;
        this.userAuthCacheManager = userAuthCacheManager;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain)
            throws ServletException, IOException {

        String header = req.getHeader(JwtUtil.AUTHORIZATION_HEADER);

        if (!StringUtils.hasText(header) || !header.startsWith(JwtUtil.BEARER_PREFIX)) {
            filterChain.doFilter(req, res);
            return;
        }

        try {
            String tokenValue = jwtUtil.subStringToken(header);

            // 블랙리스트 체크 (로그아웃 토큰 차단)
            if (redisUtil.hasKey(RedisKeys.blacklistKey(tokenValue))) {
                log.warn("[JWT] 블랙리스트 토큰 차단");
                sendError(res, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.TOKEN_BLACKLISTED);
                return;
            }

            // JWT 서명 / 만료 검증 + claims 추출
            Claims claims = jwtUtil.getUserInfoFromToken(tokenValue);

            // userId, authVersion 추출
            Long userId = Long.parseLong(claims.getSubject());
            int tokenAuthVersion = claims.get(JwtUtil.AUTH_VERSION_KEY, Integer.class);

            // Redis에서 사용자 인증 상태 조회
            UserAuthCache cache = userAuthCacheManager.get(userId);

            // 캐시 miss → DB에서 재구성
            if (cache == null) {
                cache = loadFromDbAndCache(userId);
            }

            // 삭제된 사용자 차단
            if ("DELETED".equals(cache.getStatus())) {
                log.warn("[JWT] 탈퇴/삭제된 사용자 차단 - userId: {}", userId);
                sendError(res, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.WITHDRAWN_USER);
                return;
            }

            // authVersion 불일치 → 권한 변경 이후 발급된 토큰이 아님
            if (tokenAuthVersion != cache.getAuthVersion()) {
                log.warn("[JWT] authVersion 불일치 차단 - userId: {}, token: {}, cache: {}",
                        userId, tokenAuthVersion, cache.getAuthVersion());
                sendError(res, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.INVALID_TOKEN);
                return;
            }

            // SecurityContext 저장
            setAuthentication(userId, cache);

        } catch (CustomException e) {
            log.error("[JWT] code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
            sendError(res, HttpServletResponse.SC_UNAUTHORIZED, e.getErrorCode());
            return;
        } catch (Exception e) {
            log.error("[JWT] 알 수 없는 오류", e);
            sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR);
            return;
        }

        filterChain.doFilter(req, res);
    }

    /**
     * Redis 캐시 miss 시 DB에서 User 로드 후 캐시 초기화
     * authVersion은 1로 리셋 → 기존 토큰 무효화 (재로그인 필요)
     */
    private UserAuthCache loadFromDbAndCache(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String status = user.isDeleted() ? "DELETED" : "ACTIVE";
        UserAuthCache cache = new UserAuthCache(status, user.getRole().getAuthority(), 1, user.getUsername());
        userAuthCacheManager.save(userId, cache);
        return cache;
    }

    private void setAuthentication(Long userId, UserAuthCache cache) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(cache.getRole())  // 이미 "ROLE_OWNER" 형식
        );
        UserPrincipal principal = new UserPrincipal(userId, cache.getUsername(), cache.getRole());
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    private void sendError(HttpServletResponse res, int status, ErrorCode errorCode) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(objectMapper.writeValueAsString(CommonResponse.errorBody(errorCode)));
    }
}