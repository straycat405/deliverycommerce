package com.babjo.deliverycommerce.domain.user.dto;

import com.babjo.deliverycommerce.domain.user.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserSearchResponseDto {
    private final Long userId;
    private final String username;
    private final String nickname;
    private final String email;
    private final String role;
    private final LocalDateTime createdAt;
    private final LocalDateTime deletedAt;  // 목록에서만 노출

    public UserSearchResponseDto(User user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.nickname = user.getNickname();
        this.email = user.getEmail();
        this.role = user.getRole().name();
        this.createdAt = user.getCreatedAt();
        this.deletedAt = user.getDeletedAt();

    }
}
