package com.babjo.deliverycommerce.domain.user.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserDeleteResponseDto {

    private final Long userId;
    private final String username;
    private final LocalDateTime deletedAt;

    public UserDeleteResponseDto(Long userId, String username, LocalDateTime deletedAt) {
        this.userId = userId;
        this.username = username;
        this.deletedAt = deletedAt;
    }
}
