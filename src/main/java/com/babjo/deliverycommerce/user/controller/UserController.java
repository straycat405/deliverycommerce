package com.babjo.deliverycommerce.user.controller;

import com.babjo.deliverycommerce.global.common.dto.CommonResponse;
import com.babjo.deliverycommerce.global.jwt.JwtUtil;
import com.babjo.deliverycommerce.global.redis.RedisKeys;
import com.babjo.deliverycommerce.global.redis.RedisUtil;
import com.babjo.deliverycommerce.global.security.UserPrincipal;
import com.babjo.deliverycommerce.user.dto.*;
import com.babjo.deliverycommerce.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    /**
     * POST /v1/users/signup
     * 회원가입 처리
     */
    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<SignupResponseDto>> signup (@Valid @RequestBody SignupRequestDto requestDto) {
        SignupResponseDto response = userService.signup(requestDto);
        return CommonResponse.created("회원가입 성공",response);
    }

    /**
     * POST /v1/users/login
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResponseDto>> login (
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

        return CommonResponse.ok("로그인 성공", loginResponse);
    }

    /**
     * POST /v1/users/logout
     * 로그아웃
     * AccessToken을 Redis Blacklist에 등록하고 Refresh Token을 Redis에서 삭제합니다.
     */
    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<Void>> logout(
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
        return CommonResponse.ok("로그아웃 성공",null);
    }

    /**
     * POST /v1/users/reissue
     * 토큰 재발급
     * @param refreshToken - Cookie의 "refresh_token"값
     * @param response - Header Set Cookie (new refresh token)
     */
    @PostMapping("/reissue")
    public ResponseEntity<CommonResponse<LoginResponseDto>> reissue(
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

        return CommonResponse.ok("토큰 재발급 성공", reissueResponse);
    }

    /**
     * 사용자 단건 조회
     * 권한 : MANAGER/MASTER - 전체 , OWNER/CUSTOMER - 본인만
     */

    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER') or #userId == authentication.principal.userId")
    @GetMapping("/{userId}")
    public ResponseEntity<CommonResponse<UserResponseDto>> getUser(@PathVariable long userId) {
        UserResponseDto responseDto = userService.getUser(userId);

        return CommonResponse.ok("사용자 단건 조회 성공",responseDto);
    }

    /**
     * 사용자 목록 검색
     * 권한 : MANAGER/MASTER
     */
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserSearchResponseDto>>> searchUsers(@ModelAttribute UserSearchRequestDto request) {

        log.info("사용자 목록 검색 - username={}, nickname={}, role={}, page={}, size={}, sortBy={}, isAsc={}, includeDeleted={}",
                request.getUsername(), request.getNickname(), request.getRole(), request.getPage(), request.getSize(), request.getSortBy(), request.isAsc(), request.isIncludeDeleted() );
        Page<UserSearchResponseDto> result = userService.searchUsers(request);
        return ApiResponse.ok("사용자 목록 조회 성공", result);
    }

    /**
     * 사용자 수정 (본인)
     * 권한 : 로그인 유저 본인
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserUpdateResponseDto>> updateUser(@Valid @RequestBody UserUpdateRequestDto requestDto,
                                                                   @AuthenticationPrincipal UserPrincipal principal) {

        // 토큰에서 userId 추출 (비교용)
        long userId = principal.getUserId();

        UserUpdateResponseDto responseDto = userService.updateUser(requestDto, userId);
        return ApiResponse.ok("사용자 정보 수정 성공", responseDto);
    }
}
