package com.babjo.deliverycommerce.global.security;

import com.babjo.deliverycommerce.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

public class UserDetailsImpl implements UserDetails {

    private final User user;

    public UserDetailsImpl(User user) {
        this.user = user;
    }

    // Controller / Service에서 현재 로그인 유저 정보 꺼낼때 사용
    public User getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // UserRoleEnum은 네 팀의 enum 위치에 맞게 import 경로 확인 필요
        // authority 값은 "ROLE_CUSTOMER" 형태여야 Spring Security가 인식한다
        SimpleGrantedAuthority simpleGrantedAuthority =
                new SimpleGrantedAuthority(user.getRole().getAuthority());

        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(simpleGrantedAuthority);

        return authorities;
    }

    // 아래 4개는 계정 상태 관련 메서드
    // soft delete 구조이므로 deletedAt으로 계정 활성화 여부를 체크할 수 있다
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !user.isDeleted();
    }
}
