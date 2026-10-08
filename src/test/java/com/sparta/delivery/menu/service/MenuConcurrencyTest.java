package com.sparta.delivery.menu.service;

import com.sparta.delivery.menu.dto.request.UpdateMenuRequest;
import com.sparta.delivery.menu.entity.Menu;
import com.sparta.delivery.menu.repository.MenuRepository;
import com.sparta.delivery.store.entity.Store;
import com.sparta.delivery.store.repository.StoreRepository;
import com.sparta.delivery.user.entity.User;
import com.sparta.delivery.user.repository.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.sparta.delivery.support.ServiceFixtures.assertStatus;
import static com.sparta.delivery.support.ServiceFixtures.fields;
import static org.junit.jupiter.api.Assertions.*;

@Tag("postgres")
@SpringBootTest(properties = {
    "spring.profiles.active=test",
    "spring.jpa.hibernate.ddl-auto=create",
    "spring.jpa.open-in-view=false",
    "spring.jpa.show-sql=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MenuConcurrencyTest {

    private static final String SCHEMA = "delivery_menu_lock_"
        + UUID.randomUUID().toString().replace("-", "");
    private static boolean schemaCreated;

    @Autowired
    private DataSource dataSource;
    @Autowired
    private MenuService menuService;
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long ownerId;
    private Long menuId;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) throws SQLException {
        try (Connection connection = databaseConnection();
            var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + SCHEMA);
            schemaCreated = true;
        }

        byte[] testKey = new byte[32];
        new SecureRandom().nextBytes(testKey);
        String secret = Base64.getEncoder().encodeToString(testKey);

        registry.add("spring.datasource.url", () -> requiredEnvironment("DB_TEST_URL"));
        registry.add("spring.datasource.username", () -> requiredEnvironment("DB_TEST_USERNAME"));
        registry.add("spring.datasource.password", () -> requiredEnvironment("DB_TEST_PASSWORD"));
        registry.add("spring.datasource.hikari.schema", () -> SCHEMA);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 6);
        registry.add("spring.datasource.hikari.connection-timeout", () -> 5000);
        registry.add("spring.datasource.hikari.transaction-isolation", () -> "TRANSACTION_READ_COMMITTED");
        registry.add("spring.datasource.hikari.data-source-properties.connectTimeout", () -> 5);
        registry.add("spring.datasource.hikari.data-source-properties.socketTimeout", () -> 15);
        registry.add("spring.datasource.hikari.data-source-properties.ApplicationName", () -> SCHEMA);
        registry.add("spring.datasource.hikari.connection-init-sql", () -> "SET lock_timeout = '10s'");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
        registry.add("jwt.secret", () -> secret);
    }

    @BeforeEach
    void createFixture() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            User owner = userRepository.save(new User(
                "lock-" + UUID.randomUUID().toString().substring(0, 8),
                "test-only-encoded-password",
                User.Role.OWNER
            ));
            Store store = storeRepository.save(new Store(owner, "동시성 검증 가게"));
            Menu menu = menuRepository.save(new Menu(store, "김밥", 3000, "검증용"));
            ownerId = owner.getId();
            menuId = menu.getId();
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void overlappingUpdateAndDeleteNeverRestoreMenu(boolean deleteFirst) throws Exception {
        var workers = Executors.newFixedThreadPool(2);

        try (Connection holder = dataSource.getConnection()) {
            holder.setAutoCommit(false);
            try (var statement = holder.prepareStatement(
                "SELECT id FROM " + SCHEMA + ".menus WHERE id = ? FOR UPDATE"
            )) {
                statement.setLong(1, menuId);
                try (var result = statement.executeQuery()) {
                    assertTrue(result.next());
                }
            }

            var first = workers.submit(() -> deleteFirst ? deleteMenu() : updateMenu());
            awaitLockWaiters(1);
            var second = workers.submit(() -> deleteFirst ? updateMenu() : deleteMenu());
            awaitLockWaiters(2);
            holder.commit();

            assertEquals(deleteFirst ? 204 : 200, first.get(10, TimeUnit.SECONDS));
            assertEquals(deleteFirst ? 404 : 204, second.get(10, TimeUnit.SECONDS));
            assertStatus(HttpStatus.NOT_FOUND, () -> menuService.findOne(menuId));

            try (var statement = holder.prepareStatement(
                "SELECT deleted, price FROM " + SCHEMA + ".menus WHERE id = ?"
            )) {
                statement.setLong(1, menuId);
                try (var result = statement.executeQuery()) {
                    assertTrue(result.next());
                    assertTrue(result.getBoolean("deleted"));
                    assertEquals(deleteFirst ? 3000 : 5000, result.getLong("price"));
                }
            }
            holder.rollback();
        } finally {
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private int deleteMenu() {
        try {
            menuService.delete(ownerId, menuId);
            return 204;
        } catch (ResponseStatusException exception) {
            return exception.getStatusCode().value();
        }
    }

    private int updateMenu() {
        UpdateMenuRequest request = fields(
            new UpdateMenuRequest(), "name", "떡", "price", 5000L
        );
        try {
            menuService.update(ownerId, menuId, request);
            return 200;
        } catch (ResponseStatusException exception) {
            return exception.getStatusCode().value();
        }
    }

    private void awaitLockWaiters(int expected) throws SQLException, InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            try (Connection connection = dataSource.getConnection();
                var statement = connection.prepareStatement(
                    "SELECT count(*) FROM pg_stat_activity "
                        + "WHERE application_name = ? AND wait_event_type = 'Lock'"
                )) {
                statement.setString(1, SCHEMA);
                try (var result = statement.executeQuery()) {
                    result.next();
                    if (result.getInt(1) >= expected) {
                        return;
                    }
                }
            }
            Thread.sleep(25);
        }
        fail("두 요청이 실제 PostgreSQL 행 잠금에서 대기하지 않았습니다.");
    }

    @AfterAll
    static void dropFixtureSchema() throws SQLException {
        if (schemaCreated) {
            try (Connection connection = databaseConnection();
                var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA " + SCHEMA + " CASCADE");
            }
        }
    }

    private static Connection databaseConnection() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", requiredEnvironment("DB_TEST_USERNAME"));
        properties.setProperty("password", requiredEnvironment("DB_TEST_PASSWORD"));
        properties.setProperty("connectTimeout", "5");
        properties.setProperty("socketTimeout", "15");
        return DriverManager.getConnection(requiredEnvironment("DB_TEST_URL"), properties);
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " 환경변수가 필요합니다.");
        }
        return value;
    }
}
