package com.babjo.deliverycommerce.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchRequestDto {
    private String username;
    private String nickname;
    private String role;

    @Min(0)
    private int page = 0;
    @Builder.Default
    private int size = 10;
    @Builder.Default
    private String sortBy = "createdAt";
    @Builder.Default
    private boolean asc = true;

    private boolean includeDeleted = false;

    @AssertTrue(message = "페이지 크기는 10, 30, 50만 가능합니다.")
    public boolean isValidSize() {
        return size == 10 || size == 30 || size == 50;
    }

    @AssertTrue(message = "정렬 기준은 createdAt, username, nickname만 가능합니다.")
    public boolean isValidSortBy() {
        return Set.of("createdAt", "username", "nickname").contains(sortBy);
    }
}
