package com.babjo.deliverycommerce.user.dto;

import com.babjo.deliverycommerce.user.entity.UserEnumRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
public class LoginResponseDto {

    private final Long userId;
    private final String username;
    private final String nickname;
    private final String role;
    private final String accessToken;

    @JsonIgnore // JSON 응답 제외, Controller에서만 사용
    private final String refreshToken;

    public LoginResponseDto(Long userId, String username, String nickname,
                            String role, String accessToken, String refreshToken) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.role = role;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;  // Controller에서 Cookie 세팅용
    }
}
