package com.sparta.delivery.order.dto.response;

import com.sparta.delivery.order.entity.OrderItem;
import lombok.Getter;

@Getter
public class OrderItemResponse {

    private final Long id;
    private final Long menuId;
    private final String name;
    private final long unitPrice;
    private final int quantity;
    private final long totalPrice;

    public OrderItemResponse(OrderItem item) {
        this.id = item.getId();
        this.menuId = item.getMenu().getId();
        this.name = item.getName();
        this.unitPrice = item.getUnitPrice();
        this.quantity = item.getQuantity();
        this.totalPrice = item.getTotalPrice();
    }
}