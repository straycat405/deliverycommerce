package com.babjo.deliverycommerce.global.security;

import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserResolver {


    /**
     * 현재 인증 객체에서 로그인 사용자 ID를 추출
     *
     * [클래스 생성한 이유]
     * 기존에는 컨트롤러에서 X-USER-ID 헤더로 사용자 ID를 직접 전달받는 방식으로 구현되어있음
     * 하지만 프로젝트에는 이미 Spring Security + JWT 인증 구조가 존재
     * 인증된 사용자 정보는 헤더가 아니라 SecurityContext 기반으로 가져오는 것이 더 자연스럽다 판단
     *
     * 또한 컨트롤러마다 Authentication principal을 직접 해석하면 중복 코드가 생기고
     * principal 구조가 바뀔 때 여러 컨트롤러를 함께 수정해야 하는 문제가생김
     *
     * 그래서 "현재 로그인 사용자 ID 추출" 책임을 별도 컴포넌트로 분리해
     * 컨트롤러는 인증 객체 해석 로직을 몰라도 되도록 구성
     *
     *
     *  현재 프로젝트의 JWT 인증 principal 기준은 UserPrincipal이므로
     *  resolver도 UserPrincipal을 기준으로 userId를 반환
     */
    public Long getUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return principal.getUserId();
    }
}
