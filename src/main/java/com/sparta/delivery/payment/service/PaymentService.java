package com.sparta.delivery.payment.service;

import com.sparta.delivery.order.entity.Order;
import com.sparta.delivery.order.repository.OrderRepository;
import com.sparta.delivery.payment.dto.request.CreatePaymentRequest;
import com.sparta.delivery.payment.dto.response.PaymentResponse;
import com.sparta.delivery.payment.entity.Payment;
import com.sparta.delivery.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public PaymentResponse create(
        Long customerId,
        Long orderId,
        CreatePaymentRequest request
    ) {
        Order order = orderRepository.findWithLockById(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "주문을 찾을 수 없습니다."
            ));

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인의 주문만 결제할 수 있습니다."
            );
        }

        if (paymentRepository.existsByOrderIdAndStatus(orderId, Payment.Status.PAID)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "이미 결제된 주문입니다."
            );
        }

        if (order.getStatus() != Order.Status.ORDERED) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "주문 요청 상태에서만 결제할 수 있습니다."
            );
        }

        Payment payment = new Payment(
            order,
            request.getMethod()
        );

        Payment savedPayment = paymentRepository.save(payment);

        order.markPaid();

        return new PaymentResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findAll(Long customerId) {
        return paymentRepository
            .findAllByOrderCustomerIdOrderByCreatedAtDesc(customerId)
            .stream()
            .map(PaymentResponse::new)
            .toList();
    }
}