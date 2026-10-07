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
import com.sparta.delivery.payment.entity.Payment;
import com.sparta.delivery.payment.repository.PaymentRepository;
import com.sparta.delivery.store.entity.Store;
import com.sparta.delivery.store.repository.StoreRepository;
import com.sparta.delivery.user.entity.User;
import com.sparta.delivery.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final MenuRepository menuRepository;
    private final PaymentRepository paymentRepository;
    private final Clock clock;

    @Transactional
    public OrderResponse create(
        Long customerId,
        CreateOrderRequest request
    ) {
        User customer = userRepository.findById(customerId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "회원 정보를 확인할 수 없습니다."
            ));

        Store store = storeRepository.findById(request.getStoreId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "가게를 찾을 수 없습니다."
            ));

        List<Menu> menus = new ArrayList<>();
        long totalPrice = 0;

        try {
            for (OrderItemRequest item : request.getItems()) {
                Menu menu = menuRepository
                    .findByIdAndDeletedFalse(item.getMenuId())
                    .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "메뉴를 찾을 수 없습니다."
                    ));

                if (!menu.getStore().getId().equals(store.getId())) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "같은 가게의 메뉴만 함께 주문할 수 있습니다."
                    );
                }

                long itemTotalPrice = Math.multiplyExact(
                    menu.getPrice(),
                    (long) item.getQuantity()
                );

                totalPrice = Math.addExact(
                    totalPrice,
                    itemTotalPrice
                );

                menus.add(menu);
            }
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "주문 금액이 처리 가능한 범위를 초과했습니다.",
                exception
            );
        }

        Order order = new Order(
            customer,
            store,
            request.getAddress(),
            totalPrice
        );

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();

        for (int i = 0; i < request.getItems().size(); i++) {
            OrderItem orderItem = new OrderItem(
                savedOrder,
                menus.get(i),
                request.getItems().get(i).getQuantity()
            );

            orderItems.add(orderItem);
        }

        List<OrderItem> savedItems = orderItemRepository
            .saveAll(orderItems);

        return new OrderResponse(savedOrder, savedItems);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "회원 정보를 확인할 수 없습니다."
            ));

        List<Order> orders = switch (user.getRole()) {
            case CUSTOMER -> orderRepository
                .findAllByCustomerId(userId);
            case OWNER -> orderRepository
                .findAllByStoreOwnerId(userId);
        };

        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream()
            .map(Order::getId)
            .toList();

        Map<Long, List<OrderItem>> itemsByOrderId = orderItemRepository
            .findAllByOrderIdIn(orderIds)
            .stream()
            .collect(Collectors.groupingBy(
                item -> item.getOrder().getId()
            ));

        return orders.stream()
            .map(order -> new OrderResponse(
                order,
                itemsByOrderId.getOrDefault(order.getId(), List.of())
            ))
            .toList();
    }

    @Transactional
    public OrderResponse accept(Long ownerId, Long orderId) {
        Order order = findOwnedOrderWithLock(ownerId, orderId);

        if (order.getStatus() != Order.Status.PAID) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "결제 완료된 주문만 수락할 수 있습니다."
            );
        }

        order.markAccepted();

        return new OrderResponse(
            order,
            orderItemRepository.findAllByOrderId(orderId)
        );
    }

    @Transactional
    public OrderResponse deliver(Long ownerId, Long orderId) {
        Order order = findOwnedOrderWithLock(ownerId, orderId);

        if (order.getStatus() != Order.Status.ACCEPTED) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "수락한 주문만 배달 완료할 수 있습니다."
            );
        }

        order.markDelivered();

        return new OrderResponse(
            order,
            orderItemRepository.findAllByOrderId(orderId)
        );
    }

    private Order findOwnedOrderWithLock(
        Long ownerId,
        Long orderId
    ) {
        Order order = orderRepository.findWithLockById(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "주문을 찾을 수 없습니다."
            ));

        if (!order.getStore().getOwner().getId().equals(ownerId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인 가게의 주문만 처리할 수 있습니다."
            );
        }

        return order;
    }

    @Transactional
    public OrderResponse cancel(Long customerId, Long orderId) {
        Order order = orderRepository.findWithLockById(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "주문을 찾을 수 없습니다."
            ));

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인의 주문만 취소할 수 있습니다."
            );
        }

        validateCancelableStatus(order);

        if (LocalDateTime.now(clock).isAfter(order.getCreatedAt().plusMinutes(5))) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "주문 생성 후 5분 이내에만 취소할 수 있습니다."
            );
        }

        cancelPaymentIfPaid(order);
        order.markCanceled();

        return new OrderResponse(
            order,
            orderItemRepository.findAllByOrderId(orderId)
        );
    }

    @Transactional
    public OrderResponse reject(Long ownerId, Long orderId) {
        Order order = findOwnedOrderWithLock(ownerId, orderId);
        validateCancelableStatus(order);

        cancelPaymentIfPaid(order);
        order.markRejected();

        return new OrderResponse(
            order,
            orderItemRepository.findAllByOrderId(orderId)
        );
    }

    private void validateCancelableStatus(Order order) {
        if (order.getStatus() != Order.Status.ORDERED
            && order.getStatus() != Order.Status.PAID) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "수락 전 주문만 취소하거나 거절할 수 있습니다."
            );
        }
    }

    private void cancelPaymentIfPaid(Order order) {
        if (order.getStatus() != Order.Status.PAID) {
            return;
        }

        Payment payment = paymentRepository.findByOrderId(order.getId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.CONFLICT,
                "결제 기록을 확인할 수 없습니다."
            ));

        if (payment.getStatus() != Payment.Status.PAID) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "결제 완료 상태가 아닙니다."
            );
        }

        payment.markCanceled();
    }

    @Transactional(readOnly = true)
    public OrderResponse findOne(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "주문을 찾을 수 없습니다."
            ));

        boolean isOrderCustomer = order.getCustomer()
            .getId().equals(userId);

        boolean isStoreOwner = order.getStore()
            .getOwner().getId().equals(userId);

        if (!isOrderCustomer && !isStoreOwner) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "해당 주문을 조회할 권한이 없습니다."
            );
        }

        return new OrderResponse(
            order,
            orderItemRepository.findAllByOrderId(orderId)
        );
    }
}