package com.sparta.delivery.order.entity;

import com.sparta.delivery.global.entity.BaseEntity;
import com.sparta.delivery.store.entity.Store;
import com.sparta.delivery.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private long totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public Order(
        User customer,
        Store store,
        String address,
        long totalPrice
    ) {
        this.customer = customer;
        this.store = store;
        this.address = address;
        this.totalPrice = totalPrice;
        this.status = Status.ORDERED;
    }

    public enum Status {
        ORDERED,
        PAID,
        ACCEPTED,
        DELIVERED,
        CANCELED,
        REJECTED
    }
}