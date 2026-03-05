package com.babjo.deliverycommerce.global.common.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * HTTP 요청/응답 공통 로깅 필터
 * 모든 요청에 대해 메서드, URI, IP, 응답 상태, 처리 시간을 자동 로깅함
 *
 * 세팅 완료시 자동 동작
 *
 *
 * - 로그 확인 위치
 *   콘솔 또는 logs/ 디렉토리 (Logback 설정 기준)
 *
 * - 출력 형식
 *   [REQUEST]  GET /v1/users/1 | IP=127.0.0.1
 *   [RESPONSE] GET /v1/users/1 | status=200 | 23ms
 *
 * - 주의
 *   각 도메인 Service/Controller에 중복으로 요청 로그 남기지 말기
 *   민감 정보(비밀번호, 토큰)는 로그 출력 자제
 */

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 최우선 순위 실행 필터
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        long startTime = System.currentTimeMillis();

        log.info("[REQUEST] {} {} | IP={} | UA={}",
                req.getMethod(),
                req.getRequestURI(),
                req.getRemoteAddr(),
                req.getHeader("User-Agent")
                );

        chain.doFilter(request, response); // 다음 필터로 진행

        long elapsedTime = System.currentTimeMillis() - startTime;

        log.info("[RESPONSE] {} {} | status={} | {}ms",
                req.getMethod(),
                req.getRequestURI(),
                res.getStatus(),
                elapsedTime
                );
    }

}
