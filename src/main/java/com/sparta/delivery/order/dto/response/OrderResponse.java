package com.sparta.delivery.order.dto.response;

import com.sparta.delivery.order.entity.Order;
import com.sparta.delivery.order.entity.OrderItem;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderResponse {

    private final Long id;
    private final Long customerId;
    private final Long storeId;
    private final String address;
    private final long totalPrice;
    private final Order.Status status;
    private final LocalDateTime createdAt;
    private final List<OrderItemResponse> items;

    public OrderResponse(Order order, List<OrderItem> items) {
        this.id = order.getId();
        this.customerId = order.getCustomer().getId();
        this.storeId = order.getStore().getId();
        this.address = order.getAddress();
        this.totalPrice = order.getTotalPrice();
        this.status = order.getStatus();
        this.createdAt = order.getCreatedAt();
        this.items = items.stream()
            .map(OrderItemResponse::new)
            .toList();
    }
}