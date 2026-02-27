package com.babjo.deliverycommerce.global.common.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보 없으면 null 반환 (회원가입 등 비인증 요청)
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        // 추후 CustomDetails에서 userId 꺼내는 방향으로 수정 예정
        // 임시 null 반환
        return Optional.empty();
    }

}
