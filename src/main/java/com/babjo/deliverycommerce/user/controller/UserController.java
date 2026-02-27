package com.babjo.deliverycommerce.user.controller;

import com.babjo.deliverycommerce.global.common.dto.ApiResponse;
import com.babjo.deliverycommerce.user.dto.SignupRequestDto;
import com.babjo.deliverycommerce.user.dto.SignupResponseDto;
import com.babjo.deliverycommerce.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/users")
public class UserController {

    private final UserService userService;

    /**
     * POST /v1/users/signup
     * 회원가입 처리
     * @Valid -> DTO 필드 유효성 검사 실패 시 GlobalExceptionHandler 자동 처리
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponseDto>> signup (@Valid @RequestBody SignupRequestDto requestDto) {
        SignupResponseDto response = userService.signup(requestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "회원가입 성공",response)); // 201 반환
    }
}
