package com.babjo.deliverycommerce.user.service;

import com.babjo.deliverycommerce.global.common.enums.UserEnumRole;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.babjo.deliverycommerce.global.jwt.JwtUtil;
import com.babjo.deliverycommerce.global.redis.RedisKeys;
import com.babjo.deliverycommerce.global.redis.RedisUtil;
import com.babjo.deliverycommerce.user.dto.*;
import com.babjo.deliverycommerce.user.entity.User;
import com.babjo.deliverycommerce.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    // 회원가입
    @Transactional
    public SignupResponseDto signup(SignupRequestDto dto) {

        // 비밀번호 / 비밀번호 확인 일치 검사
        if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // role 유효성 검사 (회원가입은 CUSTOMER / OWNER만 허용)
        UserEnumRole role = UserEnumRole.ofSignup(dto.getRole());

        // username 중복 검사
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }

        // email 중복 검사
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 모두 통과시 비밀번호 암호화 후 엔티티 저장
        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .role(role)
                .build();

        userRepository.save(user);

        // save()후 PK(userId)가 생성되므로 그 값으로 createdBy 세팅
        user.initCreatedBy(user.getUserId());

        log.info("[회원가입 완료] userId={}, username={}", user.getUserId(), user.getUsername());
        return new SignupResponseDto(user);
    }

    // 로그인
    public LoginResponseDto login(@Valid LoginRequestDto requestDto) {

        // username 없음 | passsword 불일치 -> LOGIN_FAILED 반환 (User Enumeration 방지)
        // 공격자로 하여금 username 존재 여부를 추론할 수 없게 하는 것이 목적
        User user = userRepository.findByUsername(requestDto.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAILED));

        // 패스워드 불일치 (LOGIN_FAILED로 통합)
        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        // Access Token 생성
        String accessToken = jwtUtil.createAccessToken(
                user.getUserId(), user.getUsername(), user.getRole().name()
        );
        log.debug("Access token={}", accessToken);

        // Refresh Token 생성
        String refreshToken = jwtUtil.createRefreshToken(user.getUserId());
        log.debug("Refresh token={}", refreshToken);

        // Redis에 refresh token 7일 지속 설정
        redisUtil.set(RedisKeys.refreshKey(user.getUserId()), refreshToken, jwtUtil.getRefreshExpiration(), TimeUnit.MILLISECONDS);

        // Cookie 세팅 없이 두 토큰 반환
        return new LoginResponseDto(
                user.getUserId(),
                user.getUsername(),
                user.getNickname(),
                user.getRole().name(),
                accessToken,
                refreshToken // Controller에서 쿠키 처리
        );
    }

    /**
     * 토큰 재발급
     * Refresh Token 검증 후 새로운 Access Token + Refresh Token 발급
     */
    public LoginResponseDto reissue(String refreshToken) {
        log.debug("reissue 호출됨, refreshToken={}", refreshToken);

        // null/빈 값 체크
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // Refresh Token 유효성 검증 + userId 추출
        Claims info;
        try {
            info = jwtUtil.getUserInfoFromToken(refreshToken);
        } catch (CustomException e) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = Long.valueOf(info.getSubject());

        // 사용자 존재 및 탈퇴 여부 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.WITHDRAWN_USER);
        }

        // Redis에서 저장된 Refresh Token 조회
        String savedToken = redisUtil.get(RedisKeys.refreshKey(userId));
        if (savedToken == null) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 비교 검증
        if (!refreshToken.equals(savedToken)) {
            // 불일치 -> 탈취 의심 -> Redis 토큰 삭제 -> 401
            redisUtil.delete(RedisKeys.refreshKey(userId));
            throw new CustomException(ErrorCode.TOKEN_MISMATCH);
        }

        // 새 Access Token + Refresh Token 발급
        String newAccessToken = jwtUtil.createAccessToken(
                user.getUserId(), user.getUsername(), user.getRole().name()
        );
        log.debug("Reissued Access token={}", newAccessToken);
        String newRefreshToken = jwtUtil.createRefreshToken(user.getUserId());
        log.debug("Reissued Refresh token={}", newRefreshToken);

        // Redis에 새 Refresh Token 갱신
        redisUtil.set(RedisKeys.refreshKey(userId), newRefreshToken, jwtUtil.getRefreshExpiration(), TimeUnit.MILLISECONDS);

        return new LoginResponseDto(
                userId,
                user.getUsername(),
                user.getNickname(),
                user.getRole().name(),
                newAccessToken,
                newRefreshToken
        );
    }

    /**
     * 사용자 단건 조회
     */
    public UserResponseDto getUser(long userId) {
        // 사용자 조회 시도
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return new UserResponseDto(user);
    }
}
