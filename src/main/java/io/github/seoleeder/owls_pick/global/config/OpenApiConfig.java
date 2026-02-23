package io.github.seoleeder.owls_pick.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        //보안 스키마 이름 지정
        String securitySchemeName = "BearerAuth";

        // 전역 보안 요구사항 설정
        // 문서 전체에 자물쇠가 적용되거나, @SecurityRequirement가 붙은 API만 선택적으로 자물쇠가 걸림
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securitySchemeName);

        // 보안 스키마 상세 설정 (JWT 토큰을 Authorization 헤더에 Bearer 방식으로 넣도록 명시)
        Components components = new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        // OpenAPI 최종 객체 생성 (문서 제목 및 설명 세팅)
        return new OpenAPI()
                .info(new Info()
                        .title("Owls Pick API 명세서")
                        .description("소셜 로그인 및 Owls Pick 서비스의 전반적인 API 명세서입니다.")
                        .version("v1.0.0"))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
