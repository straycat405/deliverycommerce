package com.babjo.deliverycommerce.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequestDto {

    @Pattern(
            regexp = "^[a-z0-9]{4,10}$",
            message = "아이디는 4자 이상 10자 이하여야 하며, 알파벳 소문자와 숫자만 사용 가능합니다."
    )
    private String username;

    // 기존 비밀번호 - 수정을 위해선 반드시 필요
    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,15}$",
            message = "비밀번호는 8자 이상 15자 이하여야 하며, 알파벳 대소문자와 숫자, 특수문자가 포함되어야 합니다."
    )
    private String prePw;

    // 새로운 비밀번호
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,15}$",
            message = "비밀번호는 8자 이상 15자 이하여야 하며, 알파벳 대소문자와 숫자, 특수문자가 포함되어야 합니다."
    )
    private String postPw;

    // 새로운 비밀번호 확인
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,15}$",
            message = "비밀번호는 8자 이상 15자 이하여야 하며, 알파벳 대소문자와 숫자, 특수문자가 포함되어야 합니다."
    )
    private String postPwConfirm;

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    private String nickname;
}
