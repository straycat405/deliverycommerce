package com.babjo.deliverycommerce.global.security;

import com.babjo.deliverycommerce.user.entity.User;
import com.babjo.deliverycommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    // Spring Security의 로그인 처리 흐름에서 자동으로 호출되는 메서드
    // UsernamePasswordAuthenticationFilter → 이 메서드 → UserDetailsImpl 반환
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new UsernameNotFoundException("Not Found " + username));

        return new UserDetailsImpl(user);
    }

    // JWT 필터에서 호출할 전용 메서드 — userId 기반 조회
    public UserDetails loadUserByUserId(Long userId) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Not Found userId: " + userId));
        return new UserDetailsImpl(user);
    }
}
