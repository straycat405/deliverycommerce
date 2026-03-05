package com.babjo.deliverycommerce.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter // 서비스/비즈니스 계층에서 DTO 값을 읽기 위해 세팅
@NoArgsConstructor // Jackson은 기본생성자 (파라미터 없는 생성자)를 통해 객체 생성 후 필드값 주입
@AllArgsConstructor // 테스트코드, 내부 변환 로직에서 명시적 생성이 필요한 경우
@Builder //테스트 코드 가독성 향상 , 객체 생성시 필드 순서 의존 제거, 선택적 필드 처리에 유리
public class LoginRequestDto {

    @NotBlank(message = "아이디를 입력해주세요.")
    private String username;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

}
