package com.sparta.delivery.payment.entity;

import com.sparta.delivery.global.entity.BaseEntity;
import com.sparta.delivery.order.entity.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "order_id",
        nullable = false,
        unique = true
    )
    private Order order;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Method method;

    public Payment(Order order, Method method) {
        this.order = order;
        this.amount = order.getTotalPrice();
        this.method = method;
    }

    public enum Method {
        CARD
    }
}