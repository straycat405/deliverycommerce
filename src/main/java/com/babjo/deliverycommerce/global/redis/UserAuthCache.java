package com.babjo.deliverycommerce.global.redis;

/**
 * Redis에 저장할 권한/상태 정보 필드 구성
 * Jackson을 이용해 Redis에 JSON 형태로 직렬화해서 저장합니다.
 */

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthCache {
    private String status;      // "ACTIVE" | "DELETED"
    private String role;        // "ROLE_OWNER" 등 GrantedAuthority 형식 사용
    private int authVersion;    // 권한/삭제 변경 시마다 ++ 증가
    private String username;    // UserPrincipal 호환
}
