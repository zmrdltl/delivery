package com.sparta.delivery.global.config;

import com.sparta.delivery.global.exception.GlobalExceptionHandler;
import com.sparta.delivery.menu.dto.request.CreateMenuRequest;
import com.sparta.delivery.menu.dto.request.UpdateMenuRequest;
import com.sparta.delivery.order.controller.OrderController;
import com.sparta.delivery.order.dto.request.CreateOrderRequest;
import com.sparta.delivery.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.json.JsonMapper;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class JsonNumberValidationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class));

    @ParameterizedTest
    @MethodSource("nonIntegerRequests")
    void rejectsFloatingPointNumbersForIntegerFields(Class<?> type, String json) {
        contextRunner.run(context -> {
            JsonMapper mapper = context.getBean(JsonMapper.class);

            assertThrows(DatabindException.class, () -> mapper.readValue(json, type));
        });
    }

    @Test
    void acceptsIntegerOrderQuantityAndMenuPrice() {
        contextRunner.run(context -> {
            JsonMapper mapper = context.getBean(JsonMapper.class);
            CreateOrderRequest order = mapper.readValue(
                """
                {"storeId": 2, "address": "서울", "items": [{"menuId": 15, "quantity": 2}]}
                """,
                CreateOrderRequest.class
            );
            CreateMenuRequest menu = mapper.readValue(
                """
                {"storeId": 2, "name": "김밥", "price": 3000}
                """,
                CreateMenuRequest.class
            );

            assertEquals(2L, order.getStoreId());
            assertEquals(15L, order.getItems().getFirst().getMenuId());
            assertEquals(2, order.getItems().getFirst().getQuantity());
            assertEquals(3000L, menu.getPrice());
        });
    }

    @Test
    void fractionalQuantityReturnsSharedBadRequestWithoutCallingService() {
        contextRunner.run(context -> {
            JsonMapper mapper = context.getBean(JsonMapper.class);
            OrderService service = mock(OrderService.class);
            var mvc = MockMvcBuilders.standaloneSetup(new OrderController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(mapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

            var response = mvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"storeId": 2, "address": "서울", "items": [{"menuId": 15, "quantity": 1.9}]}
                    """))
                .andReturn().getResponse();
            var body = mapper.readTree(response.getContentAsString());

            assertEquals(400, response.getStatus());
            assertEquals(400, body.get("status").asInt());
            assertEquals("요청 본문의 형식이나 값이 올바르지 않습니다.", body.get("message").asString());
            verifyNoInteractions(service);
        });
    }

    private static Stream<Arguments> nonIntegerRequests() {
        return Stream.of(
            Arguments.of(CreateOrderRequest.class,
                "{\"storeId\":2,\"address\":\"서울\",\"items\":[{\"menuId\":15,\"quantity\":1.9}]}"),
            Arguments.of(CreateOrderRequest.class,
                "{\"storeId\":2,\"address\":\"서울\",\"items\":[{\"menuId\":15,\"quantity\":1.0}]}"),
            Arguments.of(CreateOrderRequest.class,
                "{\"storeId\":2,\"address\":\"서울\",\"items\":[{\"menuId\":15,\"quantity\":1e0}]}"),
            Arguments.of(CreateOrderRequest.class,
                "{\"storeId\":2,\"address\":\"서울\",\"items\":[{\"menuId\":15.9,\"quantity\":1}]}"),
            Arguments.of(CreateOrderRequest.class,
                "{\"storeId\":2.9,\"address\":\"서울\",\"items\":[{\"menuId\":15,\"quantity\":1}]}"),
            Arguments.of(CreateMenuRequest.class,
                "{\"storeId\":2,\"name\":\"김밥\",\"price\":3000.9}"),
            Arguments.of(UpdateMenuRequest.class,
                "{\"name\":\"김밥\",\"price\":3000.9}")
        );
    }
}
