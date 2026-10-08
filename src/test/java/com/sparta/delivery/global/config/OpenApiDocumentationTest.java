package com.sparta.delivery.global.config;

import com.sparta.delivery.global.exception.GlobalExceptionHandler;
import com.sparta.delivery.global.security.JwtUtil;
import com.sparta.delivery.global.security.SecurityErrorHandler;
import com.sparta.delivery.menu.controller.MenuController;
import com.sparta.delivery.menu.service.MenuService;
import com.sparta.delivery.order.controller.OrderController;
import com.sparta.delivery.order.service.OrderService;
import com.sparta.delivery.payment.controller.PaymentController;
import com.sparta.delivery.payment.service.PaymentService;
import com.sparta.delivery.store.controller.StoreController;
import com.sparta.delivery.store.service.StoreService;
import com.sparta.delivery.user.controller.UserController;
import com.sparta.delivery.user.entity.User;
import com.sparta.delivery.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.sparta.delivery.support.ServiceFixtures.fields;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = OpenApiDocumentationTest.WebConfig.class)
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        DataJpaRepositoriesAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
    })
    @Import({
        OpenApiConfig.class, SecurityConfig.class, JwtUtil.class,
        SecurityErrorHandler.class, GlobalExceptionHandler.class,
        UserController.class, StoreController.class, MenuController.class,
        OrderController.class, PaymentController.class
    })
    static class WebConfig {
    }

    @DynamicPropertySource
    static void testKey(DynamicPropertyRegistry registry) {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        registry.add("jwt.secret", () -> Base64.getEncoder().encodeToString(key));
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JsonMapper mapper;
    @Autowired
    private JwtUtil jwtUtil;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private StoreService storeService;
    @MockitoBean
    private MenuService menuService;
    @MockitoBean
    private OrderService orderService;
    @MockitoBean
    private PaymentService paymentService;

    @Test
    void documentationIsAccessibleWithoutLogin() throws Exception {
        mvc.perform(get("/swagger-ui.html"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config"))
            .andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs.yaml"))
            .andExpect(status().isOk());
        assertEquals("Delivery API", specification().at("/info/title").asString());
        verifyNoInteractions(userService, storeService, menuService, orderService, paymentService);
    }

    @Test
    void documentsAllOperationsWithActualSuccessStatuses() throws Exception {
        JsonNode paths = specification().get("paths");
        int operationCount = 0;
        for (JsonNode path : paths) {
            operationCount += path.size();
        }
        assertEquals(17, operationCount);
        for (JsonNode path : paths) {
            for (JsonNode operation : path) {
                assertTrue(operation.get("responses").has("200")
                    || operation.get("responses").has("201")
                    || operation.get("responses").has("204"), operation.get("summary").asString());
            }
        }
        for (String path : List.of("/api/users", "/api/stores", "/api/menus", "/api/orders",
            "/api/orders/{orderId}/payments")) {
            JsonNode responses = paths.get(path).get("post").get("responses");
            assertTrue(responses.has("201"), path);
            assertFalse(responses.has("200"), path);
            assertTrue(responses.get("201").has("content"), path);
        }
        JsonNode deleteResponses = paths.get("/api/menus/{menuId}").get("delete").get("responses");
        assertTrue(deleteResponses.has("204"));
        assertFalse(deleteResponses.has("200"));
        assertFalse(deleteResponses.get("204").has("content"));
        assertEquals("#/components/schemas/MenuResponse", paths.get("/api/menus/{menuId}")
            .get("get").at("/responses/200/content/application~1json/schema/$ref").asString());
    }

    @Test
    void distinguishesPublicOperationsAndHidesInjectedUserIds() throws Exception {
        JsonNode spec = specification();
        JsonNode paths = spec.get("paths");
        Set<String> publicOperations = Set.of(
            "post /api/users", "post /api/users/login",
            "get /api/menus", "get /api/menus/{menuId}"
        );
        for (String path : paths.propertyNames()) {
            for (String method : paths.get(path).propertyNames()) {
                JsonNode operation = paths.get(path).get(method);
                String name = method + " " + path;
                boolean protectedOperation = !publicOperations.contains(name);
                assertEquals(protectedOperation, operation.path("security").size() > 0, name);
                if (protectedOperation) {
                    assertTrue(operation.get("security").get(0).has("bearerAuth"), name);
                    assertTrue(operation.get("responses").has("401"), name);
                    assertTrue(operation.get("responses").has("403"), name);
                }
                for (JsonNode parameter : operation.path("parameters")) {
                    assertFalse(Set.of("userId", "ownerId", "customerId")
                        .contains(parameter.get("name").asString()), name);
                }
            }
        }
        assertEquals("bearer", spec.at("/components/securitySchemes/bearerAuth/scheme").asString());
    }

    @Test
    void schemasMatchValidationAndErrorBodies() throws Exception {
        JsonNode spec = specification();
        JsonNode schemas = spec.at("/components/schemas");
        JsonNode signup = schemas.get("SignupRequest");
        assertEquals(Set.of("loginId", "password", "role"), values(signup.get("required")));
        assertFalse(signup.get("properties").has("passwordWithinByteLimit"));
        assertFalse(schemas.get("LoginRequest").get("properties").has("passwordWithinByteLimit"));
        assertEquals(4, signup.at("/properties/loginId/minLength").asInt());
        assertEquals(20, signup.at("/properties/loginId/maxLength").asInt());
        assertEquals(8, signup.at("/properties/password/minLength").asInt());
        assertEquals("integer", schemas.get("CreateMenuRequest").at("/properties/storeId/type").asString());
        assertEquals("int64", schemas.get("CreateMenuRequest").at("/properties/price/format").asString());
        assertEquals("int32", schemas.get("OrderItemRequest").at("/properties/quantity/format").asString());
        assertEquals(Set.of("OWNER", "CUSTOMER"), values(signup.at("/properties/role/enum")));
        assertEquals(Set.of("status", "message"), Set.copyOf(schemas.get("ErrorResponse")
            .get("properties").propertyNames()));
        assertEquals("#/components/schemas/ErrorResponse", spec.get("paths").get("/api/orders")
            .get("post").at("/responses/401/content/application~1json/schema/$ref").asString());
        JsonNode pageParameters = spec.get("paths").get("/api/menus").get("get").get("parameters");
        JsonNode size = StreamSupport.stream(pageParameters.spliterator(), false)
            .filter(parameter -> parameter.get("name").asString().equals("size"))
            .findFirst().orElseThrow().get("schema");
        assertEquals(1, size.get("minimum").asInt());
        assertEquals(100, size.get("maximum").asInt());
        assertEquals(10, size.get("default").asInt());
        assertEquals("integer", size.get("type").asString());
    }

    @Test
    void documentationWhitelistKeepsProtectedApisAuthenticated() throws Exception {
        mvc.perform(get("/api/orders"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
            .andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/v3/api-docs"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(orderService);
    }

    @Test
    void jwtAuthorizationStillEnforcesCustomerAndOwnerRoles() throws Exception {
        mvc.perform(post("/api/stores")
            .header(HttpHeaders.AUTHORIZATION, token(User.Role.CUSTOMER))
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"김밥집\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
        mvc.perform(post("/api/orders")
            .header(HttpHeaders.AUTHORIZATION, token(User.Role.OWNER))
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, token(User.Role.CUSTOMER)))
            .andExpect(status().isOk());
        verify(orderService).findAll(1L);
        verifyNoInteractions(storeService);
    }

    private JsonNode specification() throws Exception {
        String json = mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(json);
    }

    private String token(User.Role role) {
        User user = fields(new User("documentation-test", "test-only", role), "id", 1L);
        return "Bearer " + jwtUtil.createToken(user);
    }

    private Set<String> values(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false)
            .map(JsonNode::asString).collect(Collectors.toSet());
    }
}
