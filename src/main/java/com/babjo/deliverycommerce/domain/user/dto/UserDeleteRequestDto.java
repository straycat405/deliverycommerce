package com.babjo.deliverycommerce.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDeleteRequestDto {

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    private String password;
}
