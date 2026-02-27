package com.babjo.deliverycommerce.global.common.audit;

import com.babjo.deliverycommerce.global.security.UserDetailsImpl;
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

        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        return Optional.of(userDetails.getUser().getUserId());
    }

}
