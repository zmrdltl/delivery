package com.sparta.delivery.order.dto.response;

import com.sparta.delivery.order.entity.Order;
import com.sparta.delivery.order.entity.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(type = "string", description = "시간대 오프셋 없는 서버 로컬 시간", example = "2026-10-08T13:00:00")
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
