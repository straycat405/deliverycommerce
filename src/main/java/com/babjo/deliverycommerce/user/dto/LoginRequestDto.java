package com.babjo.deliverycommerce.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class LoginRequestDto {

    @NotBlank
    @Pattern(
            regexp = "^[a-z0-9]{4,10}$",
            message = "아이디는 4자 이상 10자 이하여야 하며, 알파벳 소문자와 숫자만 사용 가능합니다."
    )
    private String username;

    @NotBlank(message = "password는 필수 입력값입니다.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,15}$",
            message = "비밀번호는 8자 이상 15자 이하여야 하며, 알파벳 대소문자와 숫자, 특수문자만 사용 가능합니다."
    )
    private String password;

}
