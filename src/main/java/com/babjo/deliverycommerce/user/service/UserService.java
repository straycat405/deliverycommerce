package com.babjo.deliverycommerce.user.service;

import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.babjo.deliverycommerce.user.dto.SignupRequestDto;
import com.babjo.deliverycommerce.user.dto.SignupResponseDto;
import com.babjo.deliverycommerce.user.entity.User;
import com.babjo.deliverycommerce.user.entity.UserEnumRole;
import com.babjo.deliverycommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SignupResponseDto signup(SignupRequestDto dto) {

        // 비밀번호 / 비밀번호 확인 일치 검사
        if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // role 유효성 검사 (회원가입은 CUSTOMER / OWNER만 허용)
        UserEnumRole role;
        try {
            role = UserEnumRole.valueOf(dto.getRole().toUpperCase());
            if (role == UserEnumRole.MANAGER || role == UserEnumRole.MASTER) {
                throw new CustomException(ErrorCode.INVALID_ROLE);
            }
        } catch (IllegalArgumentException e) {
            // valueOf(dto.getRole()) 값 실패 - 정의되지 않은 role값
            throw new CustomException(ErrorCode.INVALID_ROLE);
        }

        // username 중복 검사
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }

        // email 중복 검사
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 모두 통과시 비밀번호 암호화 후 엔티티 저장
        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .role(role)
                .build();

        userRepository.save(user);

        // save()후 PK(userId)가 생성되므로 그 값으로 createdBy 세팅
        user.initCreatedBy(user.getUserId());

        log.info("[회원가입 완료] userId={}, username={}", user.getUserId(), user.getUsername());
        return new SignupResponseDto(user);
    }
}
