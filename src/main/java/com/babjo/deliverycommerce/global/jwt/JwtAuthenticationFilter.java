package com.babjo.deliverycommerce.global.jwt;

import com.babjo.deliverycommerce.global.common.dto.ApiResponse;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.babjo.deliverycommerce.global.security.UserDetailsImpl;
import com.babjo.deliverycommerce.user.dto.LoginRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Slf4j(topic = "JwtAuthenticationFilter")
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        setFilterProcessesUrl("/v1/users/login"); // 이 URL로 오는 요청만 해당 필터가 처리
    }

    // 로그인 요청 파싱 -> AuthenticationManager에 인증 위힘
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
        throws AuthenticationException {
        try {
            LoginRequestDto requestDto = new ObjectMapper().readValue(request.getInputStream(), LoginRequestDto.class);

            return getAuthenticationManager().authenticate(
                        new UsernamePasswordAuthenticationToken(
                                requestDto.getUsername(),
                                requestDto.getPassword(),
                                null // 인증 전이므로 권한은 null
                        )
            );

        } catch (IOException e) {
            log.error("[로그인 요청 파싱 실패] {}", e.getMessage());
            // catch 안에서 또 IOException이 날 수 있으므로 중첩 try-catch
            try {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        objectMapper.writeValueAsString(ApiResponse.error(ErrorCode.INVALID_INPUT))
                );
            } catch (IOException ex) {
                log.error("[응답 쓰기 실패] {}", ex.getMessage());
            }

            return null;


        } catch (AuthenticationException e) {
            // 인증 실패 시 unsuccessfulAuthentication()으로 위임
            // 직접 throw하면 Spring Security가 unsuccessfulAuthentication()을 호출해줌
            log.warn("[인증 실패] {}", e.getMessage());
            throw e;
        }
    }

    // 성공시 - JWT 발급 후 응답 헤더에 담아 반환
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult) throws IOException {

        // AuthenticationManager 인증 성공 후 principal에 UserDetailsImpl이 담겨 있음
        UserDetailsImpl userDetails = (UserDetailsImpl) authResult.getPrincipal();

        String accessToken = jwtUtil.createAccessToken(
                userDetails.getUser().getUserId(), // payload > userId
                userDetails.getUsername(), // payload > username
                userDetails.getUser().getRole().getAuthority() // payload > role ex)"ROLE_CUSTOMER"
        );

        // Access Token -> 응답 헤더
        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, accessToken);
        response.setStatus(HttpServletResponse.SC_OK); // 200 OK
        response.setContentType("application/json;charset=UTF-8");

        // 응답 바디 (ApiResponse 형식 통일)
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.success("로그인 성공"))
        );

        log.info("[로그인 성공] userId = {}, username = {}", userDetails.getUser().getUserId(), userDetails.getUsername());

    }

    // 실패시 - 자격증명 불일치 -> 401 반환
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.error(ErrorCode.INVALID_PASSWORD))
        );

        log.warn("[로그인 실패] {}", failed.getMessage());
    }
}
