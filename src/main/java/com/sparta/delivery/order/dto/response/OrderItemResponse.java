package com.sparta.delivery.order.dto.response;

import com.sparta.delivery.order.entity.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class OrderItemResponse {

    private final Long id;
    private final Long menuId;
    @Schema(description = "주문 당시 메뉴 이름")
    private final String name;
    @Schema(description = "주문 당시 단가")
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
