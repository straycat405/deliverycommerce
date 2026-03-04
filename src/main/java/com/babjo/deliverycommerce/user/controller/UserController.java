package com.babjo.deliverycommerce.user.controller;

import com.babjo.deliverycommerce.global.common.dto.CommonResponse;
import com.babjo.deliverycommerce.global.jwt.JwtUtil;
import com.babjo.deliverycommerce.global.redis.RedisKeys;
import com.babjo.deliverycommerce.global.redis.RedisUtil;
import com.babjo.deliverycommerce.global.security.UserPrincipal;
import com.babjo.deliverycommerce.user.dto.*;
import com.babjo.deliverycommerce.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    /**
     * POST /v1/users/signup
     */
    @Operation(
            summary = "회원가입",
            description = """                                                                                                                                                                                                         
                    - 새로운 사용자를 등록합니다.                                                                                                                                                                                           
                    - username: 4~10자, 소문자+숫자                                                                                                                                                                      
                    - password: 8~15자, 대소문자+숫자+특수문자 필수 포함                                                                                                                                                                            
                    - role: CUSTOMER, OWNER만 입력 가능                                                                                                                                                                            
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "유효성 검사 실패 / 필수값 누락 / 값 불일치"),
            @ApiResponse(responseCode = "409", description = "중복된 아이디 또는 이메일")
    })
    @SecurityRequirements
    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<SignupResponseDto>> signup(@Valid @RequestBody SignupRequestDto requestDto) {
        SignupResponseDto response = userService.signup(requestDto);
        return CommonResponse.created("회원가입 성공", response);
    }

    /**
     * POST /v1/users/login
     */
    @Operation(
            summary = "로그인",
            description = """
                    - 아이디 / 비밀번호를 검증 후 로그인합니다.
                    - 로그인에 성공하면 AccessToken, RefreshToken이 발급됩니다.
                    - Redis에 refreshToken을 7일 저장합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "유효성 검사 실패 / 필수값 누락 / 비밀번호 불일치")
    })
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResponseDto>> login(
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
     */
    @Operation(
            summary = "로그아웃",
            description = """
                    - 사용자를 로그아웃 처리합니다.
                    - AccessToken을 Redis에 블랙리스트로 등록 후,
                    - 해당 userId의 Refresh Token을 Redis 메모리에서 삭제합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "이미 로그아웃 처리된 토큰 / 유효하지 않은 토큰")
    })
    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<Void>> logout(
            @RequestHeader(JwtUtil.AUTHORIZATION_HEADER) String authHeader,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletResponse response
    ) {
        // "Bearer {token}에서 토큰만 추출
        String token = jwtUtil.subStringToken(authHeader);
        // 토큰의 userId값 추출
        long userId = principal.getUserId();
        log.info("[Logout] userId={}, username={}", userId, principal.getUsername());
        // Access Token 남은 만료시간 계산
        long duration = jwtUtil.getRemainExpiration(token);
        // Redis에 AccessToken 블랙리스트 등록
        redisUtil.set(RedisKeys.blacklistKey(token), "logout", duration, TimeUnit.MILLISECONDS);
        log.info("[Logout] 블랙리스트 등록 완료 - userId={}, 만료까지={}ms", userId, duration);
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
        return CommonResponse.ok("로그아웃 성공", null);
    }

    /**
     * POST /v1/users/reissue
     * 토큰 재발급
     *
     * @param refreshToken - Cookie의 "refresh_token"값
     * @param response     - Header Set Cookie (new refresh token)
     */
    @Operation(
            summary = "토큰 재발급",
            description = """
                    - 로그인 중인 사용자의 토큰을 인증 후 재발급합니다.
                    - 새로운 Access Token은 responseDto에,
                    - 새로운 Refresh Token은 Cookie에 세팅합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰 / 만료된 토큰 / Redis 저장값과 불일치")
    })
    @SecurityRequirements
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
     */
    @Operation(
            summary = "사용자 단건 조회",
            description = """
                    - 사용자를 조회합니다.
                    - [권한] CUSTOMER / OWNER : 본인 정보만 조회 가능
                    - [권한] MANAGER / MASTER : 전체 사용자 정보 조회 가능
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 조회 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")
    })
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER') or #userId == authentication.principal.userId")
    @GetMapping("/{userId}")
    public ResponseEntity<CommonResponse<UserResponseDto>> getUser(@PathVariable long userId) {
        UserResponseDto responseDto = userService.getUser(userId);

        return CommonResponse.ok("사용자 단건 조회 성공", responseDto);
    }

    /**
     * 사용자 목록 검색
     */
    @Operation(
            summary = "사용자 목록 검색",
            description = """
                    - 사용자 목록을 조건과 함께 페이징으로 검색합니다. (Query String 사용)
                    - [권한] MANAGER, MASTER 만 사용 가능합니다.
                    파라미터 목록
                    - 필수 파라미터는 없습니다 - 필요한 조건만 설정 가능 (기본 정렬기준 userId)
                    - username : 아이디 검색
                    - nickname : 닉네임 검색
                    - role : CUSTOMER / OWNER / MANAGER / MASTER
                    - page : 조회할 페이지 지정
                    - size : 페이지당 보여줄 데이터수 (10, 30, 50만 가능 - 기본값 10)
                    - sortBy : 정렬 기준 선택 (createdAt, username, nickname)
                    - asc : 오름차순/내림차순 선택 (기본값 true -> 오름차순)
                    - includeDeleted : 삭제처리 유저 표시여부 (기본값 false)
                    """

    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 목록 검색 성공"),
            @ApiResponse(responseCode = "400", description = "size값 오류 / sortBy값 오류"),
            @ApiResponse(responseCode = "403", description = "조회 권한 없음")
    })
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    @GetMapping
    public ResponseEntity<CommonResponse<Page<UserSearchResponseDto>>> searchUsers(@Valid @ModelAttribute UserSearchRequestDto request) {

        log.info("사용자 목록 검색 - username={}, nickname={}, role={}, page={}, size={}, sortBy={}, isAsc={}, includeDeleted={}",
                request.getUsername(), request.getNickname(), request.getRole(), request.getPage(), request.getSize(), request.getSortBy(), request.isAsc(), request.isIncludeDeleted());
        Page<UserSearchResponseDto> result = userService.searchUsers(request);
        return CommonResponse.ok("사용자 목록 조회 성공", result);
    }

    /**
     * 사용자 수정 (본인)
     */
    @Operation(
            summary = "사용자 수정 (본인)",
            description = """
                    - 사용자 정보를 수정합니다.
                    - [권한] 로그인 중인 본인 정보만 수정 가능합니다.
                    - 비밀번호 검증을 통과해야 수정이 반영됩니다. (비밀번호는 필수값)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 수정 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락 / 유효성 검증 실패 / 비밀번호 불일치"),
            @ApiResponse(responseCode = "409", description = "중복 아이디 / 이메일")
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me")
    public ResponseEntity<CommonResponse<UserUpdateResponseDto>> updateUser(@Valid @RequestBody UserUpdateRequestDto requestDto,
                                                                            @AuthenticationPrincipal UserPrincipal principal) {

        // 토큰에서 userId 추출 (비교용)
        long userId = principal.getUserId();

        UserUpdateResponseDto responseDto = userService.updateUser(requestDto, userId);
        return CommonResponse.ok("사용자 정보 수정 성공", responseDto);
    }

    /**
     * 사용자 수정 (관리자용)
     */
    @Operation(
            summary = "사용자 수정 (관리자용)",
            description = """
                    - 특정 사용자의 정보를 수정합니다.
                    - [권한] MANAGER / MASTER 만 사용 가능합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 수정 성공"),
            @ApiResponse(responseCode = "400",description = "유효성 검사 실패"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자 / 탈퇴한 사용자"),
            @ApiResponse(responseCode = "409", description = "중복 아이디 / 이메일")

    })
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    @PatchMapping("/{userId}")
    public ResponseEntity<CommonResponse<UserUpdateResponseDto>> adminUpdatedUser(@PathVariable long userId,
                                                                                  @Valid @RequestBody AdminUserUpdateRequestDto requestDto) {

        UserUpdateResponseDto responseDto = userService.adminUpdateUser(requestDto, userId);
        return CommonResponse.ok("사용자 정보 수정 성공 (관리자)", responseDto);

    }

    /**
     * 회원 탈퇴 (본인)
     */
    @Operation(
            summary = "회원 탈퇴 (본인)",
            description = """
                    - 사용자를 탈퇴 처리합니다.
                    - [권한] 로그인 유저 본인의 계정만 가능합니다.
                    - Soft Delete 정책으로, deletedAt, deletedBy 컬럼을 업데이트합니다. (물리적 삭제 X)
                    - 비밀번호 입력 검증이 필요합니다. (본인확인용)
                    - 처리가 끝나면 요청에 사용된 Access Token은 블랙리스트 처리됩니다.
                    - 로그인 상태였던 계정의 Refresh Token도 Redis 메모리에서 삭제됩니다.
                    - 로그아웃과 동일하게 Refresh Token Cookie를 만료 처리합니다. (클라이언트 쿠키 무효화)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 탈퇴 성공"),
            @ApiResponse(responseCode = "400",description = "비밀번호 불일치"),
            @ApiResponse(responseCode = "409", description = "이미 탈퇴한 사용자")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me")
    public ResponseEntity<CommonResponse<UserDeleteResponseDto>> deleteUser(@AuthenticationPrincipal UserPrincipal principal,
                                                                            @RequestHeader(JwtUtil.AUTHORIZATION_HEADER) String authHeader,
                                                                            @Valid @RequestBody UserDeleteRequestDto requestDto,
                                                                            HttpServletResponse response) {

        Long userId = principal.getUserId();

        UserDeleteResponseDto responseDto = userService.deleteUser(userId, requestDto.getPassword());

        // "Bearer {token}에서 토큰만 추출
        String token = jwtUtil.subStringToken(authHeader);
        log.info("[DeleteUserSelf] userId={}, username={}", userId, principal.getUsername());
        // Access Token 남은 만료시간 계산
        long duration = jwtUtil.getRemainExpiration(token);
        // Redis에 AccessToken 블랙리스트 등록
        redisUtil.set(RedisKeys.blacklistKey(token), "deletedUser", duration, TimeUnit.MILLISECONDS);
        log.info("[DeleteUserSelf] 블랙리스트 등록 완료 - userId={}, 만료까지={}ms", userId, duration);
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


        return CommonResponse.ok("회원 탈퇴 성공", responseDto);
    }

    /**
     * 회원 탈퇴 (관리자 전용)
     */
    @Operation(
            summary = "회원 탈퇴 처리 (관리자 전용)",
            description = """
                    - 관리자가 특정 사용자를 탈퇴 처리합니다.
                    - [권한] MANAGER / MASTER만 사용 가능합니다.
                    - 탈퇴 후 Redis에서 탈퇴 처리한 사용자의 Refresh Token이 있다면 삭제합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 탈퇴 처리 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자"),
            @ApiResponse(responseCode = "409", description = "이미 탈퇴한 사용자")
    })
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<CommonResponse<UserDeleteResponseDto>> adminDeleteUser(@AuthenticationPrincipal UserPrincipal principal,
                                                                                 @PathVariable long userId) {
        long adminUserId = principal.getUserId();

        UserDeleteResponseDto responseDto = userService.adminDeleteUser(userId, adminUserId);
        return CommonResponse.ok("회원 탈퇴 처리 성공", responseDto);
    }


    /**
     * 사용자 권한 수정
     */
    @Operation(
            summary = "사용자 권한 수정",
            description = """
                    - 특정 사용자의 권한을 수정합니다.
                    - [권한] MASTER만 사용 가능합니다.
                    - 본인의 권한은 변경 불가능합니다.
                    - 다른 MASTER의 권한은 변경 불가능합니다.
                    - 변경 가능한 권한값은 (CUSTOMER / OWNER / MANAGER)입니다.
                    - 변경 후 Redis에서 권한 변경 처리한 사용자의 Refresh Token이 있다면 삭제합니다.
                    - 변경된 권한은 재로그인 (토큰 재발급) 이후부터 적용됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 권한 변경 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음 / 본인 권한 변경 시도 / 다른 MASTER 권한 변경 시도"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자"),
            @ApiResponse(responseCode = "409", description = "이미 탈퇴한 사용자")
    })
    @PreAuthorize("hasRole('MASTER')")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<CommonResponse<AdminUserUpdateRoleResponseDto>> adminUpdateRoleUser(@AuthenticationPrincipal UserPrincipal principal,
                                                                                              @PathVariable long userId,
                                                                                              @Valid @RequestBody AdminUserUpdateRoleRequestDto requestDto) {
        long adminUserId = principal.getUserId();

        AdminUserUpdateRoleResponseDto responseDto = userService.adminUpdateRoleUser(userId, adminUserId, requestDto.getRole());
        return CommonResponse.ok("사용자 권한 변경 성공", responseDto);
    }


    /**
     * 사용자 생성
     * 권한 : MASTER만
     * RequestBody로 username, password, passwordConfirm, email, nickname 받아서 계정 생성 처리
     */
    @Operation(
            summary = "사용자 생성",
            description = """
                    - 사용자들 별도로 생성합니다.
                    - [권한] MASTER 만 사용 가능합니다.
                    - CUSTOMER / OWNER / MANAGER 권한 사용자만 생성 가능합니다.
                    - 기본적인 로직은 회원가입 처리와 동일합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "사용자 생성 성공"),
            @ApiResponse(responseCode = "400", description = "유효성 검사 실패 / 필수값 누락 / 값 불일치"),
            @ApiResponse(responseCode = "409", description = "중복된 아이디 또는 이메일")
    })
    @PreAuthorize("hasRole('MASTER')")
    @PostMapping("/admin")
    public ResponseEntity<CommonResponse<AdminSignupResponseDto>> adminSignup(@Valid @RequestBody AdminSignupRequestDto requestDto,
                                                                              @AuthenticationPrincipal UserPrincipal principal) {
        long adminUserId = principal.getUserId();

        AdminSignupResponseDto response = userService.adminSignup(requestDto, adminUserId);
        return CommonResponse.created("사용자 생성 성공", response);
    }
}
