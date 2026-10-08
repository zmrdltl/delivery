package com.sparta.delivery.order.service;

import com.sparta.delivery.menu.entity.Menu;
import com.sparta.delivery.menu.repository.MenuRepository;
import com.sparta.delivery.order.dto.response.OrderResponse;
import com.sparta.delivery.order.entity.Order;
import com.sparta.delivery.order.entity.OrderItem;
import com.sparta.delivery.order.repository.OrderItemRepository;
import com.sparta.delivery.order.repository.OrderRepository;
import com.sparta.delivery.payment.entity.Payment;
import com.sparta.delivery.payment.repository.PaymentRepository;
import com.sparta.delivery.store.entity.Store;
import com.sparta.delivery.store.repository.StoreRepository;
import com.sparta.delivery.user.entity.User;
import com.sparta.delivery.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.sparta.delivery.support.ServiceFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCancellationServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private MenuRepository menuRepository;
    @Mock
    private PaymentRepository paymentRepository;

    private OrderService orderService;
    private Order order;
    private Payment payment;
    private Menu menu;

    @BeforeEach
    void setUp() {
        Store store = store(10L, user(1L, User.Role.OWNER));
        order = order(30L, user(2L, User.Role.CUSTOMER), store, 6000);
        menu = menu(20L, store, 3000);
        payment = payment(40L, order);
        orderService = serviceAt(order.getCreatedAt().plusMinutes(4));
    }

    @ParameterizedTest
    @CsvSource({
        "ORDERED, 0", "ORDERED, 299", "ORDERED, 300",
        "PAID, 0", "PAID, 299", "PAID, 300"
    })
    void customerCanCancelUntilExactlyFiveMinutes(Order.Status status, long seconds) {
        fields(order, "status", status);
        orderService = serviceAt(order.getCreatedAt().plusSeconds(seconds));
        stubOrderWithItems();
        if (status == Order.Status.PAID) {
            when(paymentRepository.findByOrderIdAndStatus(30L, Payment.Status.PAID)).thenReturn(Optional.of(payment));
        }

        OrderResponse response = orderService.cancel(2L, 30L);

        assertEquals(Order.Status.CANCELED, response.getStatus());
        assertEquals(Order.Status.CANCELED, order.getStatus());
        assertEquals(6000, response.getTotalPrice());
        assertEquals(2, response.getItems().getFirst().getQuantity());
        if (status == Order.Status.PAID) {
            assertEquals(Payment.Status.CANCELED, payment.getStatus());
            assertEquals(6000, payment.getAmount());
            verify(paymentRepository, never()).delete(payment);
        } else {
            verifyNoInteractions(paymentRepository);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "ORDERED, 300000000001", "PAID, 300000000001",
        "ORDERED, 600000000000", "PAID, 600000000000"
    })
    void deadlineExceededLeavesOrderUnchangedWithoutQueryingPayment(Order.Status status, long nanos) {
        fields(order, "status", status);
        orderService = serviceAt(order.getCreatedAt().plusNanos(nanos));
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));

        assertStatus(HttpStatus.CONFLICT, () -> orderService.cancel(2L, 30L));

        assertEquals(status, order.getStatus());
        verifyNoInteractions(paymentRepository, orderItemRepository);
    }

    @ParameterizedTest
    @EnumSource(value = Order.Status.class, names = {"ORDERED", "PAID"})
    void ownerCanRejectBeforeAcceptanceWithoutCustomersDeadline(Order.Status status) {
        fields(order, "status", status);
        orderService = serviceAt(order.getCreatedAt().plusHours(1));
        stubOrderWithItems();
        if (status == Order.Status.PAID) {
            when(paymentRepository.findByOrderIdAndStatus(30L, Payment.Status.PAID)).thenReturn(Optional.of(payment));
        }

        OrderResponse response = orderService.reject(1L, 30L);

        assertEquals(Order.Status.REJECTED, order.getStatus());
        assertEquals(Order.Status.REJECTED, response.getStatus());
        if (status == Order.Status.PAID) {
            assertEquals(Payment.Status.CANCELED, payment.getStatus());
            assertEquals(6000, payment.getAmount());
        } else {
            verifyNoInteractions(paymentRepository);
        }
    }

    @ParameterizedTest
    @EnumSource(
        value = Order.Status.class,
        names = {"ORDERED", "PAID"},
        mode = EnumSource.Mode.EXCLUDE
    )
    void acceptedOrFinishedOrderCannotBeRejected(Order.Status status) {
        fields(order, "status", status);
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));

        assertStatus(HttpStatus.CONFLICT, () -> orderService.reject(1L, 30L));

        assertEquals(status, order.getStatus());
        verifyNoInteractions(paymentRepository, orderItemRepository);
    }

    @Test
    void otherOwnerCannotRejectPaidOrder() {
        order.markPaid();
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));

        assertStatus(HttpStatus.FORBIDDEN, () -> orderService.reject(3L, 30L));

        assertEquals(Order.Status.PAID, order.getStatus());
        verifyNoInteractions(paymentRepository, orderItemRepository);
    }

    @Test
    void otherCustomerCannotCancelPaidOrder() {
        order.markPaid();
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));

        assertStatus(HttpStatus.FORBIDDEN, () -> orderService.cancel(3L, 30L));

        assertEquals(Order.Status.PAID, order.getStatus());
        verifyNoInteractions(paymentRepository, orderItemRepository);
    }

    @Test
    void missingOrderCannotBeRejected() {
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> orderService.reject(1L, 30L));

        verifyNoInteractions(paymentRepository, orderItemRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"cancel", "reject"})
    void missingPaidPaymentPreventsBothOrderTransitions(String action) {
        order.markPaid();
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderIdAndStatus(30L, Payment.Status.PAID)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.CONFLICT, () -> transition(action));

        assertEquals(Order.Status.PAID, order.getStatus());
        verifyNoInteractions(orderItemRepository);
    }

    private OrderResponse transition(String action) {
        return action.equals("cancel")
            ? orderService.cancel(2L, 30L)
            : orderService.reject(1L, 30L);
    }

    private OrderService serviceAt(LocalDateTime now) {
        Clock clock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        return new OrderService(
            orderRepository, orderItemRepository, userRepository,
            storeRepository, menuRepository, paymentRepository, clock
        );
    }

    private void stubOrderWithItems() {
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(30L))
            .thenReturn(List.of(new OrderItem(order, menu, 2)));
    }
}
