package com.babjo.deliverycommerce.domain.user.dto;

import com.babjo.deliverycommerce.domain.user.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminSignupResponseDto {
    private final Long userId;
    private final String username;
    private final String nickname;
    private final String email;
    private final String role;
    private final LocalDateTime createdAt;

    // 엔티티 -> DTO 변환은 DTO 생성자 쪽에서 처리
    public AdminSignupResponseDto(User user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.nickname = user.getNickname();
        this.email = user.getEmail();
        this.role = user.getRole().name();
        this.createdAt = user.getCreatedAt();
    }
}
