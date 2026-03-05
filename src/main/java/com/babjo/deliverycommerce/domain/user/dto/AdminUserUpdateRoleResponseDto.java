package com.babjo.deliverycommerce.domain.user.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminUserUpdateRoleResponseDto {

    private final Long userId;
    private final String username;
    private final String role;
    private final LocalDateTime updatedAt;

    public AdminUserUpdateRoleResponseDto(Long userId, String username, String role, LocalDateTime updatedAt) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.updatedAt = updatedAt;
    }
}
