package com.babjo.deliverycommerce.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSignupRequestDto {

    @NotBlank(message = "아이디는 필수 입력값입니다.")
    @Pattern(
            regexp = "^[a-z0-9]{4,10}$",
            message = "아이디는 4자 이상 10자 이하여야 하며, 알파벳 소문자와 숫자만 사용 가능합니다."
    )
    private String username;

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,15}$",
            message = "비밀번호는 8자 이상 15자 이하여야 하며, 알파벳 대·소문자와 숫자, 특수문자가 모두 포함되어야 합니다."
    )
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수 입력값입니다.")
    private String passwordConfirm;

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "닉네임은 필수 입력값입니다.")
    private String nickname;

    @NotBlank(message = "role은 필수 입력값입니다.")
    @Pattern(regexp = "CUSTOMER|OWNER|MANAGER",
            message = "Role은 CUSTOMER,OWNER,MANAGER만 가능합니다.")
    private String role;
}
