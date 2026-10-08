package com.sparta.delivery.global.config;

import com.sparta.delivery.global.exception.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI deliveryOpenApi() {
        Components components = new Components()
            .addSecuritySchemes("bearerAuth", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT"));
        ModelConverters.getInstance().read(ErrorResponse.class)
            .forEach(components::addSchemas);

        return new OpenAPI()
            .info(new Info()
                .title("Delivery API")
                .version("0.0.1-SNAPSHOT")
                .description("ID·가격·수량은 정수로 입력합니다. 실수 표기(1.9, 1.0, 1e0)는 400입니다."))
            .components(components);
    }

    @Bean
    public OperationCustomizer errorResponses() {
        return (operation, handlerMethod) -> {
            var responses = operation.getResponses();
            responses.addApiResponse("500", new ApiResponse()
                .description("예상하지 못한 서버 오류"));

            if (operation.getSecurity() != null && !operation.getSecurity().isEmpty()) {
                responses.addApiResponse("401", new ApiResponse()
                    .description("토큰 없음·만료·오류")
                    .addHeaderObject("WWW-Authenticate", new Header()
                        .schema(new StringSchema()._default("Bearer"))));
                responses.addApiResponse("403", new ApiResponse()
                    .description("역할 또는 소유권 불일치"));
            }

            responses.forEach((code, response) -> {
                if (code.startsWith("2") && response.getContent() != null) {
                    var content = response.getContent();
                    var jsonBody = content.remove(MediaType.ALL_VALUE);
                    if (jsonBody != null) {
                        content.addMediaType(MediaType.APPLICATION_JSON_VALUE, jsonBody);
                    }
                }
                if (code.startsWith("4") || code.startsWith("5")) {
                    response.setContent(new Content().addMediaType(
                        MediaType.APPLICATION_JSON_VALUE,
                        new io.swagger.v3.oas.models.media.MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                    ));
                }
            });
            return operation;
        };
    }
}
