package com.sparta.delivery.payment.repository;

import com.sparta.delivery.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository
    extends JpaRepository<Payment, Long> {

    boolean existsByOrderIdAndStatus(Long orderId, Payment.Status status);

    Optional<Payment> findByOrderIdAndStatus(Long orderId, Payment.Status status);

    List<Payment> findAllByOrderCustomerIdOrderByCreatedAtDesc(
        Long customerId
    );
}
