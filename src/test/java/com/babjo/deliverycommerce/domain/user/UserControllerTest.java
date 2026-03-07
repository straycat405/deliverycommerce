package com.babjo.deliverycommerce.domain.user;

import com.babjo.deliverycommerce.domain.user.controller.UserController;
import com.babjo.deliverycommerce.domain.user.dto.*;
import com.babjo.deliverycommerce.domain.user.service.UserService;
import com.babjo.deliverycommerce.global.jwt.JwtUtil;
import com.babjo.deliverycommerce.global.redis.RedisUtil;
import com.babjo.deliverycommerce.global.security.CurrentUserResolver;
import com.babjo.deliverycommerce.global.security.UserPrincipal;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(UserControllerTest.MockSecurityConfig.class)
public class UserControllerTest {

    /**
     * 테스트용 보안 설정
     * - CSRF 비활성화 (production 설정과 동일)
     * - 인증 불필요 엔드포인트 허용 (signup, login, reissue)
     * - 나머지 엔드포인트는 인증 필요
     * - @PreAuthorize 동작을 위해 @EnableMethodSecurity 활성화
     */
    @TestConfiguration
    @EnableMethodSecurity
    static class MockSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    // 인증 실패시 302 리다이렉트 대신 401 Unauthorized 반환
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(
                                    "/v1/users/signup",
                                    "/v1/users/login",
                                    "/v1/users/reissue"
                            ).permitAll()
                            .anyRequest().authenticated()
                    );
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private CurrentUserResolver currentUserResolver;
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private RedisUtil redisUtil;

    // ========================
    // POST /v1/users/signup
    // ========================
    @Nested
    @DisplayName("signup")
    class Signup {

        @Test
        @DisplayName("유효한 요청 - 201 반환")
        void withValidRequest() throws Exception {
            // given
            given(userService.signup(any())).willReturn(mock(SignupResponseDto.class));

            String body = """
                    {
                        "username": "testuser",
                        "password": "Password1!",
                        "passwordConfirm": "Password1!",
                        "email": "test@test.com",
                        "nickname": "닉네임",
                        "role": "CUSTOMER"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/v1/users/signup")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("회원가입 성공"));
        }

        @Test
        @DisplayName("유효하지 않은 username 패턴 - 400 반환")
        void withInvalidUsername() throws Exception {
            // given - username에 대문자 포함
            String body = """
                    {
                        "username": "INVALID_USER",
                        "password": "Password1!",
                        "passwordConfirm": "Password1!",
                        "email": "test@test.com",
                        "nickname": "닉네임",
                        "role": "CUSTOMER"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/v1/users/signup")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("허용되지 않은 role(MANAGER) - 400 반환")
        void withUnauthorizedRole() throws Exception {
            // given - 일반 회원가입에서 MANAGER role 시도
            String body = """
                    {
                        "username": "testuser",
                        "password": "Password1!",
                        "passwordConfirm": "Password1!",
                        "email": "test@test.com",
                        "nickname": "닉네임",
                        "role": "MANAGER"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/v1/users/signup")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================
    // POST /v1/users/login
    // ========================
    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("유효한 요청 - 200 반환")
        void withValidRequest() throws Exception {
            // given
            given(userService.login(any())).willReturn(
                    new LoginResponseDto(1L, "testuser", "닉네임", "ROLE_CUSTOMER", "access-token", "refresh-token"));
            given(jwtUtil.getRefreshExpiration()).willReturn(604800000L);

            String body = """
                    {
                        "username": "testuser",
                        "password": "Password1!"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/v1/users/login")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("로그인 성공"))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }

        @Test
        @DisplayName("username 빈값 - 400 반환")
        void withBlankUsername() throws Exception {
            // given
            String body = """
                    {
                        "username": "",
                        "password": "Password1!"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/v1/users/login")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================
    // POST /v1/users/logout
    // ========================
    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @WithMockUser
        @DisplayName("인증된 사용자 - 200 반환")
        void whenAuthenticated() throws Exception {
            // given
            given(jwtUtil.subStringToken(any())).willReturn("token");
            given(currentUserResolver.getUserId(any(Authentication.class))).willReturn(1L);
            given(jwtUtil.getRemainExpiration(any())).willReturn(1000L);

            // when & then
            mockMvc.perform(post("/v1/users/logout")
                            .header("Authorization", "Bearer token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("로그아웃 성공"));
        }

        @Test
        @DisplayName("미인증 사용자 - 401 반환")
        void whenUnauthenticated() throws Exception {
            // when & then
            mockMvc.perform(post("/v1/users/logout")
                            .header("Authorization", "Bearer token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========================
    // POST /v1/users/reissue
    // ========================
    @Nested
    @DisplayName("reissue")
    class Reissue {

        @Test
        @DisplayName("유효한 쿠키 - 200 반환")
        void withValidCookie() throws Exception {
            // given
            given(userService.reissue(any())).willReturn(
                    new LoginResponseDto(1L, "testuser", "닉네임", "ROLE_CUSTOMER", "new-access", "new-refresh"));
            given(jwtUtil.getRefreshExpiration()).willReturn(604800000L);

            // when & then
            mockMvc.perform(post("/v1/users/reissue")
                            .cookie(new Cookie("refresh_token", "old-refresh-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("토큰 재발급 성공"))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }
    }

    // ========================
    // GET /v1/users/{userId}
    // ========================
    @Nested
    @DisplayName("getUser")
    class GetUser {

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("MANAGER 권한 - 200 반환")
        void withManagerRole() throws Exception {
            // given
            given(userService.getUser(anyLong())).willReturn(mock(UserResponseDto.class));

            // when & then
            mockMvc.perform(get("/v1/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("사용자 단건 조회 성공"));
        }

        @Test
        @DisplayName("CUSTOMER 권한으로 타인 조회 - 403 반환")
        void whenCustomerAccessesOther() throws Exception {
            // when & then - CUSTOMER는 자신(userId 일치)이 아닌 타인은 조회 불가
            // @WithMockUser는 standard User 객체를 생성하므로 userId 필드가 없어
            // authentication.principal.userId SpEL 평가 시 오류 발생.
            // UserPrincipal을 직접 주입하여 해결.
            mockMvc.perform(get("/v1/users/999")
                            .with(user(new UserPrincipal(1L, "testuser", "ROLE_CUSTOMER"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("미인증 사용자 - 401 반환")
        void whenUnauthenticated() throws Exception {
            // when & then
            mockMvc.perform(get("/v1/users/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========================
    // GET /v1/users
    // ========================
    @Nested
    @DisplayName("searchUsers")
    class SearchUsers {

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("MANAGER 권한 - 200 반환")
        void withManagerRole() throws Exception {
            // given
            given(userService.searchUsers(any())).willReturn(Page.empty());

            // when & then
            mockMvc.perform(get("/v1/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("사용자 목록 조회 성공"));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("CUSTOMER 권한 - 403 반환")
        void withCustomerRole() throws Exception {
            // when & then
            mockMvc.perform(get("/v1/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("미인증 사용자 - 401 반환")
        void whenUnauthenticated() throws Exception {
            // when & then
            mockMvc.perform(get("/v1/users"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========================
    // PATCH /v1/users/me
    // ========================
    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @WithMockUser
        @DisplayName("인증된 사용자 - 200 반환")
        void whenAuthenticated() throws Exception {
            // given
            given(currentUserResolver.getUserId(any(Authentication.class))).willReturn(1L);
            given(userService.updateUser(any(), anyLong())).willReturn(mock(UserUpdateResponseDto.class));

            String body = """
                    {
                        "prePw": "Password1!"
                    }
                    """;

            // when & then
            mockMvc.perform(patch("/v1/users/me")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("사용자 정보 수정 성공"));
        }

        @Test
        @WithMockUser
        @DisplayName("prePw 빈값 - 400 반환")
        void withBlankPrePw() throws Exception {
            // given - 현재 비밀번호 없이 수정 시도
            String body = """
                    {
                        "prePw": ""
                    }
                    """;

            // when & then
            mockMvc.perform(patch("/v1/users/me")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("미인증 사용자 - 401 반환")
        void whenUnauthenticated() throws Exception {
            // given
            String body = """
                    {
                        "prePw": "Password1!"
                    }
                    """;

            // when & then
            mockMvc.perform(patch("/v1/users/me")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========================
    // PATCH /v1/users/{userId}
    // ========================
    @Nested
    @DisplayName("adminUpdateUser")
    class AdminUpdateUser {

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("MANAGER 권한 - 200 반환")
        void withManagerRole() throws Exception {
            // given
            given(userService.adminUpdateUser(any(), anyLong())).willReturn(mock(UserUpdateResponseDto.class));

            String body = """
                    {
                        "nickname": "수정된닉네임"
                    }
                    """;

            // when & then
            mockMvc.perform(patch("/v1/users/1")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("사용자 정보 수정 성공 (관리자)"));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("CUSTOMER 권한 - 403 반환")
        void withCustomerRole() throws Exception {
            // given
            String body = """
                    {
                        "nickname": "수정된닉네임"
                    }
                    """;

            // when & then
            mockMvc.perform(patch("/v1/users/1")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("미인증 사용자 - 401 반환")
        void whenUnauthenticated() throws Exception {
            // given
            String body = """
                    {
                        "nickname": "수정된닉네임"
                    }
                    """;

            // when & then
            mockMvc.perform(patch("/v1/users/1")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========================
    // POST /v1/users/me (회원탈퇴)
    // ========================
    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @WithMockUser
        @DisplayName("인증된 사용자 - 200 반환")
        void whenAuthenticated() throws Exception {
            // given
            given(currentUserResolver.getUserId(any(Authentication.class))).willReturn(1L);
            given(userService.deleteUser(anyLong(), any())).willReturn(mock(UserDeleteResponseDto.class));
            given(jwtUtil.subStringToken(any())).willReturn("token");
            given(jwtUtil.getRemainExpiration(any())).willReturn(1000L);

            String body = """
                    {
                        "password": "Password1!"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/v1/users/me")
                            .contentType("application/json")
                            .content(body)
                            .header("Authorization", "Bearer token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("회원 탈퇴 성공"));
        }

        @Test
        @WithMockUser
        @DisplayName("password 빈값 - 400 반환")
        void withBlankPassword() throws Exception {
            // given - 본인 확인 비밀번호 누락
            String body = """
                    {
                        "password": ""
                    }
                    """;

            // when & then
            mockMvc.perform(post("/v1/users/me")
                            .contentType("application/json")
                            .content(body)
                            .header("Authorization", "Bearer token"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("미인증 사용자 - 401 반환")
        void whenUnauthenticated() throws Exception {
            // given
            String body = """
                    {
                        "password": "Password1!"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/v1/users/me")
                            .contentType("application/json")
                            .content(body)
                            .header("Authorization", "Bearer token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========================
    // DELETE /v1/users/{userId}
    // ========================
    @Nested
    @DisplayName("adminDeleteUser")
    class AdminDeleteUser {

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("MANAGER 권한 - 200 반환")
        void withManagerRole() throws Exception {
            // given
            given(currentUserResolver.getUserId(any(Authentication.class))).willReturn(1L);
            given(userService.adminDeleteUser(anyLong(), anyLong())).willReturn(mock(UserDeleteResponseDto.class));

            // when & then
            mockMvc.perform(delete("/v1/users/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("회원 탈퇴 처리 성공"));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("CUSTOMER 권한 - 403 반환")
        void withCustomerRole() throws Exception {
            // when & then
            mockMvc.perform(delete("/v1/users/2"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("미인증 사용자 - 401 반환")
        void whenUnauthenticated() throws Exception {
            // when & then
            mockMvc.perform(delete("/v1/users/2"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========================
    // PATCH /v1/users/{userId}/role
    // ========================
    @Nested
    @DisplayName("adminUpdateRoleUser")
    class AdminUpdateRoleUser {

        @Test
        @WithMockUser(roles = "MASTER")
        @DisplayName("MASTER 권한 - 200 반환")
        void withMasterRole() throws Exception {
            // given
            given(currentUserResolver.getUserId(any(Authentication.class))).willReturn(1L);
            given(userService.adminUpdateRoleUser(anyLong(), anyLong(), any())).willReturn(mock(AdminUserUpdateRoleResponseDto.class));

            String body = """
                    {
                        "role": "CUSTOMER"
                    }
                    """;

            // when & then
            mockMvc.perform(patch("/v1/users/2/role")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("사용자 권한 변경 성공"));
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("MANAGER 권한 - 403 반환")
        void withManagerRole() throws Exception {
            // given - MASTER만 권한 변경 가능
            String body = """
                    {
                        "role": "CUSTOMER"
                    }
                    """;

            // when & then
            mockMvc.perform(patch("/v1/users/2/role")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "MASTER")
        @DisplayName("허용되지 않은 role(MASTER) - 400 반환")
        void withInvalidRole() throws Exception {
            // given - MASTER로 변경 시도 (변경 가능 범위: CUSTOMER, OWNER, MANAGER)
            String body = """
                    {
                        "role": "MASTER"
                    }
                    """;

            // when & then
            mockMvc.perform(patch("/v1/users/2/role")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("미인증 사용자 - 401 반환")
        void whenUnauthenticated() throws Exception {
            // given
            String body = """
                    {
                        "role": "CUSTOMER"
                    }
                    """;

            // when & then
            mockMvc.perform(patch("/v1/users/2/role")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========================
    // POST /v1/users/admin
    // ========================
    @Nested
    @DisplayName("adminSignup")
    class AdminSignup {

        @Test
        @WithMockUser(roles = "MASTER")
        @DisplayName("MASTER 권한 - 201 반환")
        void withMasterRole() throws Exception {
            // given
            given(currentUserResolver.getUserId(any(Authentication.class))).willReturn(1L);
            given(userService.adminSignup(any(), anyLong())).willReturn(mock(AdminSignupResponseDto.class));

            String body = """
                    {
                        "username": "newuser1",
                        "password": "Password1!",
                        "passwordConfirm": "Password1!",
                        "email": "newuser@test.com",
                        "nickname": "새유저",
                        "role": "MANAGER"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/v1/users/admin")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("사용자 생성 성공"));
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("MANAGER 권한 - 403 반환")
        void withManagerRole() throws Exception {
            // given - MASTER만 사용자 생성 가능
            String body = """
                    {
                        "username": "newuser1",
                        "password": "Password1!",
                        "passwordConfirm": "Password1!",
                        "email": "newuser@test.com",
                        "nickname": "새유저",
                        "role": "MANAGER"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/v1/users/admin")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "MASTER")
        @DisplayName("허용되지 않은 role(MASTER) - 400 반환")
        void withInvalidRole() throws Exception {
            // given - MASTER 권한 사용자 생성 시도 (생성 가능 범위: CUSTOMER, OWNER, MANAGER)
            String body = """
                    {
                        "username": "newuser1",
                        "password": "Password1!",
                        "passwordConfirm": "Password1!",
                        "email": "newuser@test.com",
                        "nickname": "새유저",
                        "role": "MASTER"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/v1/users/admin")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("미인증 사용자 - 401 반환")
        void whenUnauthenticated() throws Exception {
            // given
            String body = """
                    {
                        "username": "newuser1",
                        "password": "Password1!",
                        "passwordConfirm": "Password1!",
                        "email": "newuser@test.com",
                        "nickname": "새유저",
                        "role": "MANAGER"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/v1/users/admin")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
    }
}