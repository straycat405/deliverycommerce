package com.babjo.deliverycommerce.global.common.audit;

/**
 * JPA Auditing - 감사 정보(createdBy, updatedBy) 자동 주입 컴포넌트
 *
 * BaseEntity의 @CreatedBy, @LastModifiedBy 필드에
 * "현재 로그인한 사용자의 userId"를 자동으로 채워줍니다.
 *
 * 별도로 createdBy / updatedBy를 set할 필요 없이,
 * save() 호출 시 JPA가 이 클래스를 통해 자동으로 값을 주입한다.
 */


import com.babjo.deliverycommerce.global.security.UserPrincipal;
import jakarta.annotation.Nonnull;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<Long> {

    @Override
    @Nonnull
    public Optional<Long> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 비로그인 or 익명 사용자 → null 반환 (회원가입 등)
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }

        // 로그인 상태 → JWT에서 파싱된 UserDetails에서 userId 꺼냄
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return Optional.of(principal.getUserId());
    }

}