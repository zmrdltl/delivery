package com.sparta.delivery.order.repository;

import com.sparta.delivery.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByCustomerId(Long customerId);

    List<Order> findAllByStoreOwnerId(Long ownerId);
}