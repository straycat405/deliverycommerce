package com.babjo.deliverycommerce.global.security;

/**
 * Spring Security 전역 설정 클래스
 *
 * [인증 없이 접근 가능한 엔드포인트]
 *   - POST /v1/users/signup   (회원가입)
 *   - POST /v1/users/login    (로그인)
 *   - POST /v1/users/reissue  (토큰 재발급)
 *   - /swagger-ui/**          (Swagger UI)
 *   - /v3/api-docs/**         (Swagger 명세)
 *   위 목록 외 모든 요청은 JWT Access Token 필수
 *
 * [권한 체크 방법]
 *   @EnableMethodSecurity 가 활성화되어 있으므로
 *   각 Controller 메서드에 @PreAuthorize 로 권한 지정할 것
 *
 *   ex) MASTER만 접근 권한 있음
 *     @PreAuthorize("hasRole('MASTER')") < 해당 방법으로
 *     @PreAuthorize("hasRole('" + UserRoleEnum.Authority.MASTER + "')") < 가능하지만 통일하는 것이 좋음
 *
 *   ex) 복수 권한
 *     @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
 *
 *   ex) 본인 여부 체크
 *     @PreAuthorize("hasAnyRole('MANAGER','MASTER') or #userId == authentication.principal.userId")
 *
 * [인증/인가 실패 응답]
 *   401 Unauthorized → JwtAuthenticationEntryPoint 에서 처리 (토큰 없음 / 유효하지 않은 토큰)
 *   403 Forbidden    → JwtAccessDeniedHandler 에서 처리 (권한 부족)
 *   별도 처리 불필요, 자동 동작
 *
 * [비밀번호 암호화]
 *   PasswordEncoder 빈이 여기 등록되어 있음 - 주입받아서 사용하세요.
 *   ex) private final PasswordEncoder passwordEncoder;
 *       passwordEncoder.encode(rawPassword);
 *
 *  - 새 public 엔드포인트 추가가 필요하면 (인증/인가 불필요 API)
 *   Git 관리자에게 요청할 것
 *   임의로 permitAll() 추가 지양
 *
 * - 주의사항
 *   CSRF 비활성화 상태임 (REST API + JWT 구조상 불필요)
 *   Session 사용 안 함 (STATELESS 설정)
 *   Security 설정 직접 수정 금지, 변경 필요시 문의 바랍니다.
 */

import com.babjo.deliverycommerce.global.jwt.JwtAccessDeniedHandler;
import com.babjo.deliverycommerce.global.jwt.JwtAuthenticationEntryPoint;
import com.babjo.deliverycommerce.global.jwt.JwtAuthorizationFilter;
import com.babjo.deliverycommerce.global.jwt.JwtUtil;
import com.babjo.deliverycommerce.global.redis.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtUtil jwtUtil;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    // JwtAuthorizationFilter도 @Component가 아니므로 직접 빈으로 등록
    @Bean
    public JwtAuthorizationFilter jwtAuthorizationFilter() {
        return new JwtAuthorizationFilter(jwtUtil, redisUtil, objectMapper);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint) // 401
                        .accessDeniedHandler(jwtAccessDeniedHandler))           // 403

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v1/users/signup",
                                "/v1/users/login",
                                "/v1/users/reissue",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // 필터 실행 순서:
                // JwtAuthorizationFilter → JwtAuthenticationFilter → UsernamePasswordAuthenticationFilter
                // 토큰 검증을 먼저 수행하고, 로그인 요청은 JwtAuthenticationFilter가 처리
                .addFilterBefore(jwtAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}