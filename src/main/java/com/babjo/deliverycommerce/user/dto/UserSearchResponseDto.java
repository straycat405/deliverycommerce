package com.babjo.deliverycommerce.user.dto;

import com.babjo.deliverycommerce.user.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserSearchResponseDto {
    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;  // 목록에서만 노출

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
