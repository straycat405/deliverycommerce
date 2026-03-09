package com.babjo.deliverycommerce.global.jwt;

/**
 * JwtAuthenticationEntryPoint / JwtAccessDeniedHandler는 Spring Security 필터 레벨에서 동작하므로
 * GlobalExceptionHandler를 거치지 않으므로 별도로 작성합니다.
 *
 * @RestControllerAdvice가 개입하기 전에 응답이 나가므로 ObjectMapper로 직접 JSON을 직렬화해서 응답합니다.
 * CommonResponse.error(ErrorCode)는 동일하게 재사용합니다.
 */

import com.babjo.deliverycommerce.global.common.dto.CommonResponse;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        // 기존 CommonResponse.error() 형식 그대로 사용
        String body = objectMapper.writeValueAsString(CommonResponse.error(ErrorCode.UNAUTHORIZED));
        response.getWriter().write(body);
    }
}