package com.babjo.deliverycommerce.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserUpdateRequestDto {

    @Pattern(
            regexp = "^[a-z0-9]{4,10}$",
            message = "아이디는 4자 이상 10자 이하여야 하며, 알파벳 소문자와 숫자만 사용 가능합니다."
    )
    private String username;

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    private String nickname;
}
