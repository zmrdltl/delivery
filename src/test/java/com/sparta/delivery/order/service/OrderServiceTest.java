package com.sparta.delivery.order.service;

import com.sparta.delivery.menu.entity.Menu;
import com.sparta.delivery.menu.repository.MenuRepository;
import com.sparta.delivery.order.dto.request.CreateOrderRequest;
import com.sparta.delivery.order.dto.request.OrderItemRequest;
import com.sparta.delivery.order.dto.response.OrderResponse;
import com.sparta.delivery.order.entity.Order;
import com.sparta.delivery.order.entity.OrderItem;
import com.sparta.delivery.order.repository.OrderItemRepository;
import com.sparta.delivery.order.repository.OrderRepository;
import com.sparta.delivery.payment.repository.PaymentRepository;
import com.sparta.delivery.store.entity.Store;
import com.sparta.delivery.store.repository.StoreRepository;
import com.sparta.delivery.user.entity.User;
import com.sparta.delivery.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.sparta.delivery.support.ServiceFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

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
    private User owner;
    private User customer;
    private Store store;
    private Menu menu;
    private Order order;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
            orderRepository, orderItemRepository, userRepository,
            storeRepository, menuRepository, paymentRepository,
            Clock.fixed(Instant.parse("2026-10-07T12:04:00Z"), ZoneOffset.UTC)
        );
        owner = user(1L, User.Role.OWNER);
        customer = user(2L, User.Role.CUSTOMER);
        store = store(10L, owner);
        menu = menu(20L, store, 3000);
        order = order(30L, customer, store, 6000);
    }

    @Test
    void createCalculatesMultipleItemsFromServerPricesAndKeepsSnapshots() {
        Menu secondMenu = menu(21L, store, 4000);
        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
        when(menuRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(menu));
        when(menuRepository.findByIdAndDeletedFalse(21L)).thenReturn(Optional.of(secondMenu));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation ->
            fields(invocation.getArgument(0), "id", 30L)
        );
        List<OrderItem> persistedItems = new ArrayList<>();
        when(orderItemRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<OrderItem> items = invocation.getArgument(0);
            persistedItems.addAll(items);
            return items;
        });
        CreateOrderRequest request = request(List.of(item(20L, 2), item(21L, 1)));

        OrderResponse response = orderService.create(2L, request);
        menu.update("바뀐 메뉴", 9000, null);
        menu.delete();

        assertEquals(2L, response.getCustomerId());
        assertEquals(10L, response.getStoreId());
        assertEquals(Order.Status.ORDERED, response.getStatus());
        assertEquals(10000, response.getTotalPrice());
        assertEquals(2, response.getItems().size());
        assertEquals("메뉴20", response.getItems().getFirst().getName());
        assertEquals(3000, response.getItems().getFirst().getUnitPrice());
        assertEquals(6000, response.getItems().getFirst().getTotalPrice());
        assertEquals(4000, response.getItems().get(1).getTotalPrice());

        when(orderRepository.findById(30L))
            .thenReturn(Optional.of(persistedItems.getFirst().getOrder()));
        when(orderItemRepository.findAllByOrderId(30L)).thenReturn(persistedItems);

        OrderResponse history = orderService.findOne(2L, 30L);

        assertEquals(10000, history.getTotalPrice());
        assertEquals("메뉴20", history.getItems().getFirst().getName());
        assertEquals(3000, history.getItems().getFirst().getUnitPrice());
        assertEquals(6000, history.getItems().getFirst().getTotalPrice());
    }

    @Test
    void missingCustomerDoesNotCreateOrder() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.UNAUTHORIZED, () ->
            orderService.create(2L, request(List.of(item(20L, 1))))
        );

        assertNoOrderWrites();
    }

    @Test
    void missingStoreDoesNotCreateOrder() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(storeRepository.findById(10L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () ->
            orderService.create(2L, request(List.of(item(20L, 1))))
        );

        assertNoOrderWrites();
    }

    @Test
    void missingOrDeletedMenuDoesNotCreateOrder() {
        stubCustomerAndStore();
        when(menuRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () ->
            orderService.create(2L, request(List.of(item(20L, 1))))
        );

        assertNoOrderWrites();
    }

    @Test
    void menusFromAnotherStoreAreRejectedWithoutSavingOrder() {
        stubCustomerAndStore();
        Menu foreignMenu = menu(20L, store(11L, owner), 3000);
        when(menuRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(foreignMenu));

        assertStatus(HttpStatus.BAD_REQUEST, () ->
            orderService.create(2L, request(List.of(item(20L, 1))))
        );

        assertNoOrderWrites();
    }

    @Test
    void overflowingItemAmountIsRejectedWithoutSavingOrder() {
        stubCustomerAndStore();
        Menu expensive = menu(20L, store, Long.MAX_VALUE);
        when(menuRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(expensive));

        assertStatus(HttpStatus.BAD_REQUEST, () ->
            orderService.create(2L, request(List.of(item(20L, 2))))
        );

        assertNoOrderWrites();
    }

    @Test
    void overflowingCombinedAmountIsRejectedWithoutSavingOrder() {
        stubCustomerAndStore();
        when(menuRepository.findByIdAndDeletedFalse(20L))
            .thenReturn(Optional.of(menu(20L, store, Long.MAX_VALUE)));
        when(menuRepository.findByIdAndDeletedFalse(21L))
            .thenReturn(Optional.of(menu(21L, store, 1)));

        assertStatus(HttpStatus.BAD_REQUEST, () ->
            orderService.create(2L, request(List.of(item(20L, 1), item(21L, 1))))
        );

        assertNoOrderWrites();
    }

    @Test
    void customerListGroupsItemsUnderTheirOwnOrders() {
        Order secondOrder = order(31L, customer, store, 4000);
        OrderItem firstItem = new OrderItem(order, menu, 2);
        OrderItem secondItem = new OrderItem(secondOrder, menu(21L, store, 4000), 1);
        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(orderRepository.findAllByCustomerId(2L)).thenReturn(List.of(order, secondOrder));
        when(orderItemRepository.findAllByOrderIdIn(List.of(30L, 31L)))
            .thenReturn(List.of(secondItem, firstItem));

        List<OrderResponse> response = orderService.findAll(2L);

        assertEquals(2, response.size());
        assertEquals(20L, response.getFirst().getItems().getFirst().getMenuId());
        assertEquals(6000, response.getFirst().getItems().getFirst().getTotalPrice());
        assertEquals(21L, response.get(1).getItems().getFirst().getMenuId());
        assertEquals(4000, response.get(1).getItems().getFirst().getTotalPrice());
        verify(orderRepository, never()).findAllByStoreOwnerId(anyLong());
    }

    @Test
    void ownerListUsesOwnStoreOrders() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(orderRepository.findAllByStoreOwnerId(1L)).thenReturn(List.of(order));
        when(orderItemRepository.findAllByOrderIdIn(List.of(30L)))
            .thenReturn(List.of(new OrderItem(order, menu, 2)));

        List<OrderResponse> response = orderService.findAll(1L);

        assertEquals(30L, response.getFirst().getId());
        assertEquals(6000, response.getFirst().getItems().getFirst().getTotalPrice());
        verify(orderRepository, never()).findAllByCustomerId(anyLong());
    }

    @Test
    void emptyOrderListDoesNotQueryItems() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(orderRepository.findAllByCustomerId(2L)).thenReturn(List.of());

        assertTrue(orderService.findAll(2L).isEmpty());

        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void missingUserCannotListOrders() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.UNAUTHORIZED, () -> orderService.findAll(2L));

        verifyNoInteractions(orderRepository, orderItemRepository);
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 2L})
    void detailAllowsOrderCustomerAndStoreOwner(long userId) {
        when(orderRepository.findById(30L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(30L))
            .thenReturn(List.of(new OrderItem(order, menu, 2)));

        OrderResponse response = orderService.findOne(userId, 30L);

        assertEquals(30L, response.getId());
        assertEquals(6000, response.getItems().getFirst().getTotalPrice());
    }

    @Test
    void unrelatedUserCannotReadOrderItems() {
        when(orderRepository.findById(30L)).thenReturn(Optional.of(order));

        assertStatus(HttpStatus.FORBIDDEN, () -> orderService.findOne(3L, 30L));

        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void missingOrderDetailReturnsNotFound() {
        when(orderRepository.findById(30L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> orderService.findOne(2L, 30L));
    }

    @Test
    void ownerAcceptsPaidOrder() {
        order.markPaid();
        stubLockedOrderWithItems();

        OrderResponse response = orderService.accept(1L, 30L);

        assertEquals(Order.Status.ACCEPTED, order.getStatus());
        assertEquals(Order.Status.ACCEPTED, response.getStatus());
    }

    @Test
    void ownerDeliversAcceptedOrder() {
        order.markAccepted();
        stubLockedOrderWithItems();

        OrderResponse response = orderService.deliver(1L, 30L);

        assertEquals(Order.Status.DELIVERED, order.getStatus());
        assertEquals(Order.Status.DELIVERED, response.getStatus());
    }

    @Test
    void customerCancelsOwnUnpaidOrder() {
        stubLockedOrderWithItems();

        OrderResponse response = orderService.cancel(2L, 30L);

        assertEquals(Order.Status.CANCELED, order.getStatus());
        assertEquals(Order.Status.CANCELED, response.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = Order.Status.class, names = "PAID", mode = EnumSource.Mode.EXCLUDE)
    void acceptRejectsOtherStatesWithoutChangingOrder(Order.Status status) {
        fields(order, "status", status);
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));

        assertStatus(HttpStatus.CONFLICT, () -> orderService.accept(1L, 30L));

        assertEquals(status, order.getStatus());
        verifyNoInteractions(orderItemRepository);
    }

    @ParameterizedTest
    @EnumSource(value = Order.Status.class, names = "ACCEPTED", mode = EnumSource.Mode.EXCLUDE)
    void deliverRejectsOtherStatesWithoutChangingOrder(Order.Status status) {
        fields(order, "status", status);
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));

        assertStatus(HttpStatus.CONFLICT, () -> orderService.deliver(1L, 30L));

        assertEquals(status, order.getStatus());
        verifyNoInteractions(orderItemRepository);
    }

    @ParameterizedTest
    @EnumSource(
        value = Order.Status.class,
        names = {"ORDERED", "PAID"},
        mode = EnumSource.Mode.EXCLUDE
    )
    void cancelRejectsOtherStatesWithoutChangingOrder(Order.Status status) {
        fields(order, "status", status);
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));

        assertStatus(HttpStatus.CONFLICT, () -> orderService.cancel(2L, 30L));

        assertEquals(status, order.getStatus());
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void otherOwnerCannotAcceptPaidOrder() {
        order.markPaid();
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));

        assertStatus(HttpStatus.FORBIDDEN, () -> orderService.accept(3L, 30L));

        assertEquals(Order.Status.PAID, order.getStatus());
    }

    @Test
    void otherOwnerCannotDeliverAcceptedOrder() {
        order.markAccepted();
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));

        assertStatus(HttpStatus.FORBIDDEN, () -> orderService.deliver(3L, 30L));

        assertEquals(Order.Status.ACCEPTED, order.getStatus());
    }

    @Test
    void otherCustomerCannotCancelUnpaidOrder() {
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));

        assertStatus(HttpStatus.FORBIDDEN, () -> orderService.cancel(3L, 30L));

        assertEquals(Order.Status.ORDERED, order.getStatus());
    }

    @Test
    void missingOrderCannotBeAccepted() {
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> orderService.accept(1L, 30L));
    }

    @Test
    void missingOrderCannotBeDelivered() {
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> orderService.deliver(1L, 30L));
    }

    @Test
    void missingOrderCannotBeCanceled() {
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> orderService.cancel(2L, 30L));
    }

    private void stubCustomerAndStore() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
    }

    private void stubLockedOrderWithItems() {
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(30L))
            .thenReturn(List.of(new OrderItem(order, menu, 2)));
    }

    private CreateOrderRequest request(List<OrderItemRequest> items) {
        return fields(
            new CreateOrderRequest(), "storeId", 10L,
            "address", "테스트 주소", "items", items
        );
    }

    private OrderItemRequest item(long menuId, int quantity) {
        return fields(new OrderItemRequest(), "menuId", menuId, "quantity", quantity);
    }

    private void assertNoOrderWrites() {
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository, never()).saveAll(anyList());
    }
}
