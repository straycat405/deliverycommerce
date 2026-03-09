package com.babjo.deliverycommerce.global.jpa;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // @EnableJpaAuditing 을 메인 클래스에 붙이는 경우도 있지만
    // 테스트 환경에서 충돌 가능성 있음. 별도 Config 클래스로 분리해서 작성합니다.
}
