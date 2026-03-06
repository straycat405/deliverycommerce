package com.babjo.deliverycommerce.global.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * JWT 토큰에서 userId를, Redis UserAuthCache에서 username과 role을 가져와 인증 객체로 사용합니다.
 * * JwtAuthorizationFilter에서 토큰 검증 성공 시 SecurityContext에 저장되며,
 * 컨트롤러에서 @AuthenticationPrincipal로 주입받아 사용합니다.
 * *
 * 사용예시
 * @GetMapping("/my-info")
 * public ResponseEntity<?> getMyInfo(
 *         @AuthenticationPrincipal UserPrincipal principal
 *     Long userId = principal.getUserId();
 *     String username = principal.getUsername();
 *     String role = principal.getRole();
 *     // ...
 */

@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails{
    private final Long userId;
    private final String username;
    private final String role;

    // UserDetails 필수 오버라이드
    // 현재 구성은 토큰 생성 시점에 role 직접 넣고 있으므로 사용하지 않습니다.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    //UserDetails 필수 오버라이드 - JWT 방식이므로 불필요
    @Override
    public String getPassword() {
        return null;
    }


}