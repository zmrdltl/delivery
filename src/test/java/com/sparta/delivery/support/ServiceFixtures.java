package com.sparta.delivery.support;

import com.sparta.delivery.menu.entity.Menu;
import com.sparta.delivery.order.entity.Order;
import com.sparta.delivery.payment.entity.Payment;
import com.sparta.delivery.store.entity.Store;
import com.sparta.delivery.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ServiceFixtures {

    private ServiceFixtures() {
    }

    public static <T> T fields(T target, Object... values) {
        for (int i = 0; i < values.length; i += 2) {
            ReflectionTestUtils.setField(target, (String) values[i], values[i + 1]);
        }
        return target;
    }

    public static User user(long id, User.Role role) {
        return fields(new User("user" + id, "encoded-password", role), "id", id);
    }

    public static Store store(long id, User owner) {
        return fields(new Store(owner, "가게" + id), "id", id);
    }

    public static Menu menu(long id, Store store, long price) {
        return fields(new Menu(store, "메뉴" + id, price, "설명"), "id", id);
    }

    public static Order order(long id, User customer, Store store, long total) {
        return fields(
            new Order(customer, store, "테스트 주소", total),
            "id", id,
            "createdAt", LocalDateTime.of(2026, 10, 7, 12, 0)
        );
    }

    public static Payment payment(long id, Order order) {
        return fields(
            new Payment(order, Payment.Method.CARD),
            "id", id,
            "createdAt", LocalDateTime.of(2026, 10, 7, 12, 0)
        );
    }

    public static void assertStatus(HttpStatus status, Runnable action) {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            action::run
        );
        assertEquals(status, exception.getStatusCode());
    }
}
