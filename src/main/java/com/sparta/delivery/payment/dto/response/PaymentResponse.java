package com.sparta.delivery.payment.dto.response;

import com.sparta.delivery.payment.entity.Payment;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PaymentResponse {

    private final Long id;
    private final Long orderId;
    private final long amount;
    private final Payment.Method method;
    private final Payment.Status status;
    private final LocalDateTime createdAt;

    public PaymentResponse(Payment payment) {
        this.id = payment.getId();
        this.orderId = payment.getOrder().getId();
        this.amount = payment.getAmount();
        this.method = payment.getMethod();
        this.status = payment.getStatus();
        this.createdAt = payment.getCreatedAt();
    }
}