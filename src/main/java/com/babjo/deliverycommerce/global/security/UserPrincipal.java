package com.babjo.deliverycommerce.global.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * JWT 토큰에서 추출한 인증된 사용자 정보를 담는 객체입니다.
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
public class UserPrincipal {
    private final Long userId;
    private final String username;
    private final String role;
}