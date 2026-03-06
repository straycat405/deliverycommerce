package com.babjo.deliverycommerce.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserUpdateRoleRequestDto {

    @NotBlank(message = "role은 필수 입력값입니다.")
    @Pattern(regexp = "CUSTOMER|OWNER|MANAGER",
            message = "Role은 CUSTOMER,OWNER,MANAGER만 가능합니다.")
    private String role;
}
