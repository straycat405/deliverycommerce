package com.babjo.deliverycommerce.user.controller;

import com.babjo.deliverycommerce.global.common.dto.ApiResponse;
import com.babjo.deliverycommerce.global.common.entity.BaseEntity;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.babjo.deliverycommerce.global.jwt.JwtUtil;
import com.babjo.deliverycommerce.global.redis.RedisKeys;
import com.babjo.deliverycommerce.global.redis.RedisUtil;
import com.babjo.deliverycommerce.global.security.UserPrincipal;
import com.babjo.deliverycommerce.user.dto.LoginRequestDto;
import com.babjo.deliverycommerce.user.dto.LoginResponseDto;
import com.babjo.deliverycommerce.user.dto.SignupRequestDto;
import com.babjo.deliverycommerce.user.dto.SignupResponseDto;
import com.babjo.deliverycommerce.user.entity.User;
import com.babjo.deliverycommerce.user.repository.UserRepository;
import com.babjo.deliverycommerce.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final UserRepository userRepository;

    /**
     * POST /v1/users/signup
     * 회원가입 처리
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponseDto>> signup (@Valid @RequestBody SignupRequestDto requestDto) {
        SignupResponseDto response = userService.signup(requestDto);
        return ApiResponse.created("회원가입 성공",response);
    }

    /**
     * POST /v1/users/login
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login (
            @Valid @RequestBody LoginRequestDto requestDto,
            HttpServletResponse response // Cookie-Refresh Token 세팅용
    ) {
        LoginResponseDto loginResponse = userService.login(requestDto);

        // Http Cookie 설정
        ResponseCookie cookie = ResponseCookie.from("refresh_token", loginResponse.getRefreshToken())
                .httpOnly(true)
                .path("/")
                .maxAge(jwtUtil.getRefreshExpiration() / 1000) // ms -> 초 변환
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.ok("로그인 성공", loginResponse);
    }

    /**
     * POST /v1/users/logout
     * 로그아웃
     * AccessToken을 Redis Blacklist에 등록하고 Refresh Token을 Redis에서 삭제합니다.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(JwtUtil.AUTHORIZATION_HEADER) String authHeader,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletResponse response
    ) {
        // "Bearer {token}에서 토큰만 추출
        String token = jwtUtil.subStringToken(authHeader);
        Claims info = jwtUtil.getUserInfoFromToken(token);
        // 토큰의 userId값 추출
        long userId = principal.getUserId();
        // Access Token 남은 만료시간 계산
        long duration = jwtUtil.getRemainExpiration(token);
        // Redis에 AccessToken 블랙리스트 등록
        redisUtil.set(RedisKeys.blacklistKey(token),"logout", duration, TimeUnit.MILLISECONDS);
        // Refresh Token 삭제
        redisUtil.delete(RedisKeys.refreshKey(userId));
        // Refresh Token 쿠키 만료 (응답 쿠키 설정)
        // Http Cookie 설정
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0) // ms -> 초 변환
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 응답 반환
        return ApiResponse.ok("로그아웃 성공",null);
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<LoginResponseDto>> reissue(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        LoginResponseDto reissueResponse = userService.reissue(refreshToken);

        // Http Cookie 설정
        ResponseCookie cookie = ResponseCookie.from("refresh_token", reissueResponse.getRefreshToken())
                .httpOnly(true)
                .path("/")
                .maxAge(jwtUtil.getRefreshExpiration() / 1000)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.ok("토큰 재발급 성공", reissueResponse);
    }
}
