package com.babjo.deliverycommerce.global.common.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // JWT 인증 스키마 이름
        String securityScheme = "bearer";

        return new OpenAPI()
                // API 기본정보
                .info(apiInfo())
                // 서버 URL을 지정하지 않으면 springdoc이 현재 접속 서버 URL을 자동 사용
                // 전역 Security 설정 (API 자물쇠 아이콘 표시)
                .addSecurityItem(new SecurityRequirement().addList(securityScheme))
                // Security 스키마 정의
                .components(new Components()
                        .addSecuritySchemes(securityScheme,
                                new SecurityScheme()
                                        .name(securityScheme)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT") // UI 힌트 (실제 검증 X)
                                        .description("JWT Access Token 입력 (Bearer prefix 제외)")
                        )
                );
    }
    // API 기본정보 정의
    private Info apiInfo() {
        return new Info()
                .title("Delivery Commerce - 배달 주문 관리 플랫폼 API")
                .description("""
                        ## 배달 주문 관리 플랫폼 백엔드 API
                        
                        ### 인증 방식
                        - JWT Bearer Token 인증
                        - 로그인 후 발급받은 Access Token을 Authorization 헤더에 포함
                        - ex) Authorization : Bearer eyJhbGciOiJI.....
                        
                        ### 권한 체계
                        |권한|설명|
                        |-|-|
                        |CUSTOMER|일반 고객|
                        |OWNER|가게 사장님|
                        |MANAGER|관리자|
                        |MASTER|최고관리자|
                        """)
                .version("v1.0.0")
                .contact(new Contact()
                        .name("밥조 (7조)")
                        .email("babjo777@gmail.com")
                );
    }
}
