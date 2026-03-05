package com.babjo.deliverycommerce.user.service;

import com.babjo.deliverycommerce.global.common.enums.UserEnumRole;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.babjo.deliverycommerce.global.jwt.JwtUtil;
import com.babjo.deliverycommerce.global.redis.RedisKeys;
import com.babjo.deliverycommerce.global.redis.RedisUtil;
import com.babjo.deliverycommerce.global.redis.UserAuthCache;
import com.babjo.deliverycommerce.global.redis.UserAuthCacheManager;
import com.babjo.deliverycommerce.user.dto.*;
import com.babjo.deliverycommerce.user.entity.User;
import com.babjo.deliverycommerce.user.repository.UserQueryRepository;
import com.babjo.deliverycommerce.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final UserQueryRepository userQueryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final UserAuthCacheManager userAuthCacheManager;


    // 회원가입
    @Transactional
    public SignupResponseDto signup(SignupRequestDto requestDto) {

        // 비밀번호 / 비밀번호 확인 일치 검사
        if (!requestDto.getPassword().equals(requestDto.getPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // role 유효성 검사 (회원가입은 CUSTOMER / OWNER만 허용)
        UserEnumRole role = UserEnumRole.ofSignup(requestDto.getRole());

        // username 중복 검사 (삭제된 사용자 포함 - UNIQUE조건 정합성)
        if (userRepository.existsByUsernameAll(requestDto.getUsername())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }

        // email 중복 검사 (삭제된 사용자 포함 - UNIQUE조건 정합성)
        if (userRepository.existsByEmailAll(requestDto.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 모두 통과시 비밀번호 암호화 후 엔티티 저장
        User user = User.builder()
                .username(requestDto.getUsername())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .email(requestDto.getEmail())
                .nickname(requestDto.getNickname())
                .role(role)
                .build();

        userRepository.save(user);

        // save()후 PK(userId)가 생성되므로 그 값으로 createdBy 세팅
        user.initCreatedBy(user.getUserId());

        log.info("[회원가입 완료] userId={}, username={}", user.getUserId(), user.getUsername());
        return new SignupResponseDto(user);
    }

    // 로그인
    public LoginResponseDto login(LoginRequestDto requestDto) {

        // username 없음 | passsword 불일치 -> LOGIN_FAILED 반환 (User Enumeration 방지)
        // 공격자로 하여금 username 존재 여부를 추론할 수 없게 하는 것이 목적
        User user = userRepository.findByUsername(requestDto.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAILED));

        // User Enumeration 방지
        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        // 패스워드 불일치 (LOGIN_FAILED로 통합)
        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        /**
         * AccessToken 생성
         * 1) Redis에서 UserAuthCache 조회 (authVersion 추출 용도)
         * 2) 없으면 새로 생성 후 save
         */
        UserAuthCache cache = userAuthCacheManager.get(user.getUserId());
        if (cache == null) {
            cache = new UserAuthCache("ACTIVE", user.getRole().getAuthority(), 1, user.getUsername());
        }
        // 로그인 사용자 캐시 30일 보관 (로그인 때마다 갱신)
        userAuthCacheManager.saveWithTtl(user.getUserId(), cache, 30,TimeUnit.DAYS);

        String accessToken = jwtUtil.createAccessToken(user.getUserId(), cache.getAuthVersion());

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

        Long userId = null;

        // Refresh Token 유효성 검증 + userId 추출
        Claims info;
        try {
            info = jwtUtil.getUserInfoFromToken(refreshToken);
            userId = Long.valueOf(info.getSubject());
        } catch (CustomException | NumberFormatException e) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

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

        UserAuthCache cache = userAuthCacheManager.get(user.getUserId());
        if (cache == null) {
            cache = new UserAuthCache("ACTIVE", user.getRole().getAuthority(), 1, user.getUsername());
        }
        userAuthCacheManager.saveWithTtl(user.getUserId(), cache, 30, TimeUnit.DAYS); // TTL 갱신

        String newAccessToken = jwtUtil.createAccessToken(user.getUserId(), cache.getAuthVersion());

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

        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.WITHDRAWN_USER);
        }

        return new UserResponseDto(user);
    }

    /**
     * 사용자 목록 검색
     */
    public Page<UserSearchResponseDto> searchUsers(UserSearchRequestDto request) {
        // Pageable 생성
        Sort sort = request.isAsc() ? Sort.by(request.getSortBy()).ascending() : Sort.by(request.getSortBy()).descending();
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        // repository 조회
        Page<User> users = userQueryRepository.searchUsers(
                request.getUsername(),
                request.getNickname(),
                request.getRole(),
                request.isIncludeDeleted(),
                pageable
        );

        // dto 변환
        return users.map(UserSearchResponseDto::new);
    }

    /**
     * 사용자 정보 수정 (본인)
     */
    @Transactional
    public UserUpdateResponseDto updateUser(UserUpdateRequestDto requestDto, long userId) {

        // 기존 유저 정보
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.WITHDRAWN_USER);
        }

        // prePw 불일치시 예외 (항상 검증)
        if (!passwordEncoder.matches(requestDto.getPrePw(), user.getPassword())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // username 중복시 예외 던짐
        if (requestDto.getUsername() != null
                && !requestDto.getUsername().equals(user.getUsername())
                && userRepository.existsByUsernameAll(requestDto.getUsername())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }
        // postPw / postPwConfirm 값 있을시 검증
        if (requestDto.getPostPw() != null) {
            if (!requestDto.getPostPw().equals(requestDto.getPostPwConfirm())) {
                throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
            }
        }
        // email 중복일시 예외던짐
        if (requestDto.getEmail() != null
                && !requestDto.getEmail().equals(user.getEmail())
                && userRepository.existsByEmailAll(requestDto.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        // 다 통과하면 사용자 정보 수정
        user.updateUser(
                requestDto.getUsername(),
                // passwordEncoder.encode(null) 시 NPE 발생
                requestDto.getPostPw() != null ? passwordEncoder.encode(requestDto.getPostPw()) : null,
                requestDto.getEmail(),
                requestDto.getNickname()
        );

        userRepository.flush();

        // username이 변경된 경우 cache의 username 동기화
        if (requestDto.getUsername() != null) {
            UserAuthCache currentCache = userAuthCacheManager.get(userId);
            if (currentCache != null) {
                userAuthCacheManager.saveWithTtl(userId, new UserAuthCache(
                        currentCache.getStatus(), // 나머지는 그대로
                        currentCache.getRole(),
                        currentCache.getAuthVersion(),
                        user.getUsername()  // 변경 완료된 username값
                ),30,  TimeUnit.DAYS);
            }
        }



        return new UserUpdateResponseDto(user);
    }

    /**
     * 사용자 정보 수정 (관리자용)
     */
    @Transactional
    public UserUpdateResponseDto adminUpdateUser(AdminUserUpdateRequestDto requestDto, long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.WITHDRAWN_USER);
        }

        //username 변경시 본인 제외 중복체크
        if (requestDto.getUsername() != null
                && !requestDto.getUsername().equals(user.getUsername())
                && userRepository.existsByUsernameAll(requestDto.getUsername())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }
        //email 변경시 본인 제외 중복체크
        if (requestDto.getEmail() != null
                && !requestDto.getEmail().equals(user.getEmail())
                && userRepository.existsByEmailAll(requestDto.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        //user.updateUser()호출
        user.updateUser(
                requestDto.getUsername(),
                null, // 비밀번호는 변경하지 않음
                requestDto.getEmail(),
                requestDto.getNickname()
        );

        userRepository.flush();

        // username이 변경된 경우 cache의 username 동기화
        if (requestDto.getUsername() != null) {
            UserAuthCache currentCache = userAuthCacheManager.get(userId);
            if (currentCache != null) {
                userAuthCacheManager.save(userId, new UserAuthCache(
                        currentCache.getStatus(),
                        currentCache.getRole(),
                        currentCache.getAuthVersion(),
                        user.getUsername()  // 변경 완료된 값
                ));
            }
        }

        return new UserUpdateResponseDto(user);
    }

    /**
     * 사용자 삭제 처리 (본인)
     */
    @Transactional
    public UserDeleteResponseDto deleteUser(Long userId, String password) {
        // 사용자 정보 추출
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.WITHDRAWN_USER);
        }

        //비밀번호 검증 (틀리면 예외)
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 맞으면 해당 userId 레코드 delete처리
        user.delete(userId);

        // Redis auth cache → DELETED 처리 (즉시 차단 - AccessToken 지속시간 15분동안)
        UserAuthCache currentCache = userAuthCacheManager.get(userId);
        int currentVersion = (currentCache != null) ? currentCache.getAuthVersion() : 1;
        String currentRole  = (currentCache != null) ? currentCache.getRole() : user.getRole().getAuthority();
        userAuthCacheManager.saveWithTtl(userId,
                new UserAuthCache("DELETED", currentRole, currentVersion, user.getUsername()), 15,TimeUnit.MINUTES);

        // Refresh Token 삭제
        redisUtil.delete(RedisKeys.refreshKey(userId));

        // 응답에 userId,username,deletedAt 담아서 반환
        return new UserDeleteResponseDto(user.getUserId(), user.getUsername(), user.getDeletedAt());
    }

    /**
     * 사용자 삭제 처리 (관리자 전용)
     */
    @Transactional
    public UserDeleteResponseDto adminDeleteUser(long userId, long adminUserId) {
        // 본인 userId 삭제하는 상황 방지 (용도 분리)
        if (userId == adminUserId) {
            throw new CustomException(ErrorCode.CANNOT_DELETE_SELF);
        }

        // 사용자 정보 추출
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.WITHDRAWN_USER);
        }

        // 해당 유저가 MASTER면 예외처리
        if (user.getRole() == UserEnumRole.MASTER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 해당 userId 레코드 delete처리
        user.delete(adminUserId);

        // Redis auth cache → DELETED 처리 (즉시 차단)
        UserAuthCache currentCache = userAuthCacheManager.get(userId);
        int currentVersion = (currentCache != null) ? currentCache.getAuthVersion() : 1;
        String currentRole  = (currentCache != null) ? currentCache.getRole() : user.getRole().getAuthority();
        userAuthCacheManager.save(userId,
                new UserAuthCache("DELETED", currentRole, currentVersion, user.getUsername()));

        // Redis에 삭제한 사용자의 refresh token이 존재한다면 삭제
        redisUtil.delete(RedisKeys.refreshKey(userId));

        // 응답에 userId,username,deletedAt 담아서 반환
        return new UserDeleteResponseDto(user.getUserId(), user.getUsername(), user.getDeletedAt());
    }

    /**
     * 사용자 권한 변경
     */
    @Transactional
    public AdminUserUpdateRoleResponseDto adminUpdateRoleUser(long userId, long adminUserId, String role) {
        // 본인이면 예외처리
        if (userId == adminUserId) {
            throw new CustomException(ErrorCode.CANNOT_UPDATE_ROLE_SELF);
        }

        // 유저 조회
        User user =  userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        // 탈퇴 유저는 권한 변경 불가
        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.WITHDRAWN_USER);
        }

        // 해당 유저가 MASTER면 예외처리
        if (user.getRole() == UserEnumRole.MASTER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 사용자 권한 수정
        user.updateRoleUser(UserEnumRole.of(role));
        userRepository.flush();

        // Redis auth cache 즉시 갱신 (authVersion 증가 → 기존 토큰 즉시 무효화)
        UserAuthCache currentCache = userAuthCacheManager.get(userId);
        int newAuthVersion = (currentCache != null) ? currentCache.getAuthVersion() + 1 : 1;
        userAuthCacheManager.save(userId,
                new UserAuthCache("ACTIVE", user.getRole().getAuthority(), newAuthVersion, user.getUsername()));

        // Refresh Token 삭제 → 재로그인 강제
        redisUtil.delete(RedisKeys.refreshKey(userId));

        return new AdminUserUpdateRoleResponseDto(user.getUserId(), user.getUsername(), user.getRole().name(), user.getUpdatedAt());
    }

    /**
     * 사용자 생성 - 관리자용
     * ROLE은 CUSTOMER/OWNER/MANAGER만 설정 가능
     */
    @Transactional
    public AdminSignupResponseDto adminSignup(AdminSignupRequestDto requestDto, long adminUserId) {

        // 비밀번호 / 비밀번호 확인 일치 검사
        if (!requestDto.getPassword().equals(requestDto.getPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // role 유효성 검사 (회원가입은 CUSTOMER / OWNER / MANAGER 만 허용)
        UserEnumRole role = UserEnumRole.ofAdminSignup(requestDto.getRole());

        // username 중복 검사 (삭제된 사용자 포함 - UNIQUE조건 정합성)
        if (userRepository.existsByUsernameAll(requestDto.getUsername())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }

        // email 중복 검사 (삭제된 사용자 포함 - UNIQUE조건 정합성)
        if (userRepository.existsByEmailAll(requestDto.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 모두 통과시 비밀번호 암호화 후 엔티티 저장
        User user = User.builder()
                .username(requestDto.getUsername())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .email(requestDto.getEmail())
                .nickname(requestDto.getNickname())
                .role(role)
                .build();

        userRepository.save(user);

        // save()후 PK(userId)가 생성되므로 그 값으로 createdBy 세팅
        user.initCreatedBy(adminUserId);

        log.info("[회원 생성 완료] userId={}, username={}", user.getUserId(), user.getUsername());
        return new AdminSignupResponseDto(user);
    }
}
