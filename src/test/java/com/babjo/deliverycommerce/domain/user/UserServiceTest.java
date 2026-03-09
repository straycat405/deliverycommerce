package com.babjo.deliverycommerce.domain.user;

import com.babjo.deliverycommerce.domain.user.dto.*;
import com.babjo.deliverycommerce.domain.user.entity.User;
import com.babjo.deliverycommerce.domain.user.repository.UserQueryRepository;
import com.babjo.deliverycommerce.domain.user.repository.UserRepository;
import com.babjo.deliverycommerce.domain.user.service.UserService;
import com.babjo.deliverycommerce.global.common.enums.UserEnumRole;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.babjo.deliverycommerce.global.jwt.JwtUtil;
import com.babjo.deliverycommerce.global.redis.RedisUtil;
import com.babjo.deliverycommerce.global.redis.UserAuthCache;
import com.babjo.deliverycommerce.global.redis.UserAuthCacheManager;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserQueryRepository userQueryRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private RedisUtil redisUtil;
    @Mock private UserAuthCacheManager userAuthCacheManager;

    @InjectMocks private UserService userService;

    // 테스트용 User 생성 헬퍼 (createForTest - 리플렉션으로 userId 주입)
    private User createUser(Long userId, String username, UserEnumRole role) {
        return User.createForTest(userId, username, username + "@test.com", "닉네임", role);
    }

    // 탈퇴 처리된 User 생성 헬퍼
    private User createDeletedUser(Long userId, String username, UserEnumRole role) {
        User user = createUser(userId, username, role);
        user.delete(userId);
        return user;
    }

    @Nested
    @DisplayName("signup")
    class Signup {

        private SignupRequestDto createSignupRequest(String username, String password, String passwordConfirm) {
            return SignupRequestDto.builder()
                    .username(username)
                    .password(password)
                    .passwordConfirm(passwordConfirm)
                    .email(username + "@test.com")
                    .nickname("닉네임")
                    .role("CUSTOMER")
                    .build();
        }

        @Test
        @DisplayName("성공 - SignupResponseDto 반환")
        void withValidRequest() {
            // given
            SignupRequestDto request = createSignupRequest("testuser1", "Password1!", "Password1!");

            given(userRepository.existsByUsernameAll(request.getUsername())).willReturn(false);
            given(userRepository.existsByEmailAll(request.getEmail())).willReturn(false);
            given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPw");

            // when
            SignupResponseDto response = userService.signup(request);

            // then
            assertThat(response.getUsername()).isEqualTo("testuser1");
        }

        @Test
        @DisplayName("비밀번호 확인 불일치 - PASSWORD_MISMATCH 예외 발생")
        void withPasswordMismatch() {
            // given
            SignupRequestDto request = createSignupRequest("testuser1", "Password1!", "WrongPassword1!");

            // when & then
            assertThatThrownBy(() -> userService.signup(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PASSWORD_MISMATCH);
        }

        @Test
        @DisplayName("username 중복 - DUPLICATE_USERNAME 예외 발생")
        void withDuplicateUsername() {
            // given
            SignupRequestDto request = createSignupRequest("testuser1", "Password1!", "Password1!");

            given(userRepository.existsByUsernameAll(request.getUsername())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.signup(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATE_USERNAME);
        }

        @Test
        @DisplayName("email 중복 - DUPLICATE_EMAIL 예외 발생")
        void withDuplicateEmail() {
            // given
            SignupRequestDto request = createSignupRequest("testuser1", "Password1!", "Password1!");

            given(userRepository.existsByUsernameAll(request.getUsername())).willReturn(false);
            given(userRepository.existsByEmailAll(request.getEmail())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.signup(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        private LoginRequestDto createLoginRequest(String username, String password) {
            return LoginRequestDto.builder()
                    .username(username)
                    .password(password)
                    .build();
        }

        @Test
        @DisplayName("성공 - LoginResponseDto 반환")
        void withValidCredentials() {
            // given
            User user = createUser(1L, "testuser1", UserEnumRole.CUSTOMER);
            LoginRequestDto request = createLoginRequest("testuser1", "Password1!");

            given(userRepository.findByUsername("testuser1")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("Password1!", user.getPassword())).willReturn(true);
            given(userAuthCacheManager.get(1L)).willReturn(new UserAuthCache("ACTIVE", "ROLE_CUSTOMER", 1, "testuser1"));
            given(jwtUtil.createAccessToken(1L, 1)).willReturn("accessToken");
            given(jwtUtil.createRefreshToken(1L)).willReturn("refreshToken");
            given(jwtUtil.getRefreshExpiration()).willReturn(604800000L);

            // when
            LoginResponseDto response = userService.login(request);

            // then
            assertThat(response.getUsername()).isEqualTo("testuser1");
            assertThat(response.getAccessToken()).isEqualTo("accessToken");
        }

        @Test
        @DisplayName("존재하지 않는 username - LOGIN_FAILED 예외 발생")
        void withNonExistentUsername() {
            // given
            LoginRequestDto request = createLoginRequest("notexist", "Password1!");

            given(userRepository.findByUsername("notexist")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.LOGIN_FAILED);
        }

        @Test
        @DisplayName("탈퇴한 사용자로 로그인 시도 - LOGIN_FAILED 예외 발생")
        void withWithdrawnUser() {
            // given
            User user = createDeletedUser(1L, "testuser1", UserEnumRole.CUSTOMER);
            LoginRequestDto request = createLoginRequest("testuser1", "Password1!");

            given(userRepository.findByUsername("testuser1")).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.LOGIN_FAILED);
        }

        @Test
        @DisplayName("비밀번호 불일치 - LOGIN_FAILED 예외 발생")
        void withPasswordMismatch() {
            // given
            User user = createUser(1L, "testuser1", UserEnumRole.CUSTOMER);
            LoginRequestDto request = createLoginRequest("testuser1", "WrongPw!");

            given(userRepository.findByUsername("testuser1")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("WrongPw!", user.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.LOGIN_FAILED);
        }
    }

    @Nested
    @DisplayName("reissue")
    class Reissue {

        @Test
        @DisplayName("성공 - LoginResponseDto 반환")
        void withValidRefreshToken() {
            // given
            User user = createUser(1L, "testuser1", UserEnumRole.CUSTOMER);
            String refreshToken = "validRefreshToken";

            Claims mockClaims = mock(Claims.class);
            given(mockClaims.getSubject()).willReturn("1");
            given(jwtUtil.getUserInfoFromToken(refreshToken)).willReturn(mockClaims);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(redisUtil.get("refresh:1")).willReturn(refreshToken);
            given(userAuthCacheManager.get(1L)).willReturn(new UserAuthCache("ACTIVE", "ROLE_CUSTOMER", 1, "testuser1"));
            given(jwtUtil.createAccessToken(1L, 1)).willReturn("newAccessToken");
            given(jwtUtil.createRefreshToken(1L)).willReturn("newRefreshToken");
            given(jwtUtil.getRefreshExpiration()).willReturn(604800000L);

            // when
            LoginResponseDto response = userService.reissue(refreshToken);

            // then
            assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
            assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");
            verify(redisUtil).set(eq("refresh:1"), eq("newRefreshToken"), anyLong(), eq(TimeUnit.MILLISECONDS));
        }

        @Test
        @DisplayName("refreshToken null - INVALID_REFRESH_TOKEN 예외 발생")
        void withNullRefreshToken() {
            // when & then
            assertThatThrownBy(() -> userService.reissue(null))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("유효하지 않은 토큰 - INVALID_REFRESH_TOKEN 예외 발생")
        void withInvalidToken() {
            // given
            String invalidToken = "invalidRefreshToken";
            given(jwtUtil.getUserInfoFromToken(invalidToken))
                    .willThrow(new CustomException(ErrorCode.INVALID_TOKEN));

            // when & then
            assertThatThrownBy(() -> userService.reissue(invalidToken))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("탈퇴한 사용자 - WITHDRAWN_USER 예외 발생")
        void withWithdrawnUser() {
            // given
            User user = createDeletedUser(1L, "testuser1", UserEnumRole.CUSTOMER);
            String refreshToken = "validRefreshToken";
            Claims mockClaims = mock(Claims.class);
            given(mockClaims.getSubject()).willReturn("1");
            given(jwtUtil.getUserInfoFromToken(refreshToken)).willReturn(mockClaims);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> userService.reissue(refreshToken))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.WITHDRAWN_USER);
        }

        @Test
        @DisplayName("Redis에 토큰 없음 - INVALID_REFRESH_TOKEN 예외 발생")
        void whenTokenNotStoredInRedis() {
            // given
            User user = createUser(1L, "testuser1", UserEnumRole.CUSTOMER);
            String refreshToken = "validRefreshToken";
            Claims mockClaims = mock(Claims.class);
            given(mockClaims.getSubject()).willReturn("1");
            given(jwtUtil.getUserInfoFromToken(refreshToken)).willReturn(mockClaims);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(redisUtil.get("refresh:1")).willReturn(null);

            // when & then
            assertThatThrownBy(() -> userService.reissue(refreshToken))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("Redis 저장값과 불일치 - TOKEN_MISMATCH 예외 발생")
        void withMismatchedToken() {
            // given
            User user = createUser(1L, "testuser1", UserEnumRole.CUSTOMER);
            String refreshToken = "providedToken";
            Claims mockClaims = mock(Claims.class);
            given(mockClaims.getSubject()).willReturn("1");
            given(jwtUtil.getUserInfoFromToken(refreshToken)).willReturn(mockClaims);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(redisUtil.get("refresh:1")).willReturn("differentToken");

            // when & then
            assertThatThrownBy(() -> userService.reissue(refreshToken))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TOKEN_MISMATCH);
        }
    }

    @Nested
    @DisplayName("getUser")
    class GetUser {

        @Test
        @DisplayName("성공 - UserResponseDto 반환")
        void withExistingUser() {
            // given
            User user = createUser(1L, "testuser1", UserEnumRole.CUSTOMER);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when
            UserResponseDto response = userService.getUser(1L);

            // then
            assertThat(response.getUsername()).isEqualTo("testuser1");
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        private UserUpdateRequestDto createUpdateRequest(String username, String prePw,
                                                         String postPw, String postPwConfirm, String email) {
            return UserUpdateRequestDto.builder()
                    .username(username)
                    .prePw(prePw)
                    .postPw(postPw)
                    .postPwConfirm(postPwConfirm)
                    .email(email)
                    .build();
        }

        @Test
        @DisplayName("성공 - UserUpdateResponseDto 반환")
        void withValidRequest() {
            // given
            User user = createUser(1L, "testuser1", UserEnumRole.CUSTOMER);
            UserUpdateRequestDto request = createUpdateRequest(null, "Password1!", null, null, null);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("Password1!", user.getPassword())).willReturn(true);

            // when
            UserUpdateResponseDto response = userService.updateUser(request, 1L);

            // then
            assertThat(response.getUsername()).isEqualTo("testuser1");
        }

        @Test
        @DisplayName("기존 비밀번호 불일치 - PASSWORD_MISMATCH 예외 발생")
        void withIncorrectCurrentPassword() {
            // given
            User user = createUser(1L, "testuser1", UserEnumRole.CUSTOMER);
            UserUpdateRequestDto request = createUpdateRequest(null, "WrongPw!", null, null, null);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("WrongPw!", user.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.updateUser(request, 1L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PASSWORD_MISMATCH);
        }

        @Test
        @DisplayName("새 username 중복 - DUPLICATE_USERNAME 예외 발생")
        void withDuplicateUsername() {
            // given
            User user = createUser(1L, "testuser1", UserEnumRole.CUSTOMER);
            UserUpdateRequestDto request = createUpdateRequest("duplicate", "Password1!", null, null, null);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("Password1!", user.getPassword())).willReturn(true);
            given(userRepository.existsByUsernameAll("duplicate")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.updateUser(request, 1L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATE_USERNAME);
        }

        @Test
        @DisplayName("새 비밀번호 확인 불일치 - PASSWORD_MISMATCH 예외 발생")
        void withNewPasswordMismatch() {
            // given
            User user = createUser(1L, "testuser1", UserEnumRole.CUSTOMER);
            UserUpdateRequestDto request = createUpdateRequest(null, "Password1!", "NewPass1!", "WrongConfirm1!", null);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("Password1!", user.getPassword())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.updateUser(request, 1L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PASSWORD_MISMATCH);
        }

        @Test
        @DisplayName("새 email 중복 - DUPLICATE_EMAIL 예외 발생")
        void withDuplicateEmail() {
            // given
            User user = createUser(1L, "testuser1", UserEnumRole.CUSTOMER);
            UserUpdateRequestDto request = createUpdateRequest(null, "Password1!", null, null, "dup@test.com");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("Password1!", user.getPassword())).willReturn(true);
            given(userRepository.existsByEmailAll("dup@test.com")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.updateUser(request, 1L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("성공 - UserDeleteResponseDto 반환")
        void withValidPassword() {
            // given
            User user = createUser(1L, "testuser1", UserEnumRole.CUSTOMER);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("Password1!", user.getPassword())).willReturn(true);

            // when
            UserDeleteResponseDto response = userService.deleteUser(1L, "Password1!");

            // then
            assertThat(response.getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("비밀번호 불일치 - INVALID_PASSWORD 예외 발생")
        void withInvalidPassword() {
            // given
            User user = createUser(1L, "testuser1", UserEnumRole.CUSTOMER);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("WrongPw!", user.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.deleteUser(1L, "WrongPw!"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_PASSWORD);
        }
    }

    @Nested
    @DisplayName("adminDeleteUser")
    class AdminDeleteUser {

        @Test
        @DisplayName("성공 - UserDeleteResponseDto 반환")
        void withValidTarget() {
            // given
            User user = createUser(2L, "targetuser", UserEnumRole.CUSTOMER);
            given(userRepository.findById(2L)).willReturn(Optional.of(user));

            // when
            UserDeleteResponseDto response = userService.adminDeleteUser(2L, 1L);

            // then
            assertThat(response.getUserId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("본인 계정 삭제 시도 - CANNOT_DELETE_SELF 예외 발생")
        void whenDeletingSelf() {
            // when & then
            assertThatThrownBy(() -> userService.adminDeleteUser(1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CANNOT_DELETE_SELF);
        }

        @Test
        @DisplayName("MASTER 권한 대상 - FORBIDDEN 예외 발생")
        void whenTargetIsMaster() {
            // given
            User masterUser = createUser(2L, "masteruser", UserEnumRole.MASTER);
            given(userRepository.findById(2L)).willReturn(Optional.of(masterUser));

            // when & then
            assertThatThrownBy(() -> userService.adminDeleteUser(2L, 1L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("adminUpdateRoleUser")
    class AdminUpdateRoleUser {

        @Test
        @DisplayName("성공 - AdminUserUpdateRoleResponseDto 반환")
        void withValidTarget() {
            // given
            User user = createUser(2L, "targetuser", UserEnumRole.CUSTOMER);
            given(userRepository.findById(2L)).willReturn(Optional.of(user));

            // when
            AdminUserUpdateRoleResponseDto response = userService.adminUpdateRoleUser(2L, 1L, "OWNER");

            // then
            assertThat(response.getRole()).isEqualTo("OWNER");
        }

        @Test
        @DisplayName("본인 권한 변경 시도 - CANNOT_UPDATE_ROLE_SELF 예외 발생")
        void whenUpdatingSelf() {
            // when & then
            assertThatThrownBy(() -> userService.adminUpdateRoleUser(1L, 1L, "OWNER"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CANNOT_UPDATE_ROLE_SELF);
        }

        @Test
        @DisplayName("MASTER 권한 대상 - FORBIDDEN 예외 발생")
        void whenTargetIsMaster() {
            // given
            User masterUser = createUser(2L, "masteruser", UserEnumRole.MASTER);
            given(userRepository.findById(2L)).willReturn(Optional.of(masterUser));

            // when & then
            assertThatThrownBy(() -> userService.adminUpdateRoleUser(2L, 1L, "OWNER"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("adminSignup")
    class AdminSignup {

        private AdminSignupRequestDto createAdminSignupRequest(String username, String password,
                                                               String passwordConfirm, String role) {
            return AdminSignupRequestDto.builder()
                    .username(username)
                    .password(password)
                    .passwordConfirm(passwordConfirm)
                    .email(username + "@test.com")
                    .nickname("닉네임")
                    .role(role)
                    .build();
        }

        @Test
        @DisplayName("성공 - AdminSignupResponseDto 반환")
        void withValidRequest() {
            // given
            AdminSignupRequestDto request = createAdminSignupRequest("manager1", "Password1!", "Password1!", "MANAGER");

            given(userRepository.existsByUsernameAll(request.getUsername())).willReturn(false);
            given(userRepository.existsByEmailAll(request.getEmail())).willReturn(false);
            given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPw");

            // when
            AdminSignupResponseDto response = userService.adminSignup(request, 1L);

            // then
            assertThat(response.getUsername()).isEqualTo("manager1");
            assertThat(response.getRole()).isEqualTo("MANAGER");
        }
    }
}
