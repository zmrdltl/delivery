package com.sparta.delivery.payment.repository;

import com.sparta.delivery.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository
    extends JpaRepository<Payment, Long> {

    boolean existsByOrderId(Long orderId);

    List<Payment> findAllByOrderCustomerIdOrderByCreatedAtDesc(
        Long customerId
    );
}
