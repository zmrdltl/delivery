package com.sparta.delivery.payment.service;

import com.sparta.delivery.order.entity.Order;
import com.sparta.delivery.order.repository.OrderRepository;
import com.sparta.delivery.payment.dto.request.CreatePaymentRequest;
import com.sparta.delivery.payment.dto.response.PaymentResponse;
import com.sparta.delivery.payment.entity.Payment;
import com.sparta.delivery.payment.repository.PaymentRepository;
import com.sparta.delivery.store.entity.Store;
import com.sparta.delivery.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static com.sparta.delivery.support.ServiceFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderRepository orderRepository;

    private PaymentService paymentService;
    private Order order;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, orderRepository);
        Store store = store(10L, user(1L, User.Role.OWNER));
        order = order(30L, user(2L, User.Role.CUSTOMER), store, 7000);
    }

    @Test
    void paymentUsesOrderAmountAndMarksOrderPaid() {
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation ->
            fields(invocation.getArgument(0), "id", 40L)
        );

        PaymentResponse response = paymentService.create(2L, 30L, cardRequest());

        assertEquals(7000, response.getAmount());
        assertEquals(30L, response.getOrderId());
        assertEquals(Payment.Method.CARD, response.getMethod());
        assertEquals(Payment.Status.PAID, response.getStatus());
        assertEquals(Order.Status.PAID, order.getStatus());
    }

    @Test
    void otherCustomerCannotPayOrChangeOrder() {
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));

        assertStatus(HttpStatus.FORBIDDEN, () ->
            paymentService.create(3L, 30L, cardRequest())
        );

        assertEquals(Order.Status.ORDERED, order.getStatus());
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void duplicatePaymentDoesNotCreateAnotherRecord() {
        order.markPaid();
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(30L)).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () ->
            paymentService.create(2L, 30L, cardRequest())
        );

        assertEquals(Order.Status.PAID, order.getStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @ParameterizedTest
    @EnumSource(value = Order.Status.class, names = "ORDERED", mode = EnumSource.Mode.EXCLUDE)
    void nonOrderedStatesCannotBePaid(Order.Status status) {
        fields(order, "status", status);
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));

        assertStatus(HttpStatus.CONFLICT, () ->
            paymentService.create(2L, 30L, cardRequest())
        );

        assertEquals(status, order.getStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void missingOrderDoesNotCreatePayment() {
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () ->
            paymentService.create(2L, 30L, cardRequest())
        );

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void failedPaymentSaveLeavesOrderUnpaid() {
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenThrow(
            new DataIntegrityViolationException("payment save failed")
        );

        assertThrows(DataIntegrityViolationException.class, () ->
            paymentService.create(2L, 30L, cardRequest())
        );

        assertEquals(Order.Status.ORDERED, order.getStatus());
    }

    @Test
    void historyReturnsAuthenticatedCustomersPayments() {
        Payment payment = payment(40L, order);
        when(paymentRepository.findAllByOrderCustomerIdOrderByCreatedAtDesc(2L))
            .thenReturn(List.of(payment));

        List<PaymentResponse> response = paymentService.findAll(2L);

        assertEquals(1, response.size());
        assertEquals(30L, response.getFirst().getOrderId());
        assertEquals(7000, response.getFirst().getAmount());
    }

    @Test
    void canceledPaymentsRemainInHistoryWithTheirOriginalAmount() {
        Payment canceled = payment(40L, order);
        canceled.markCanceled();
        order.markCanceled();
        when(paymentRepository.findAllByOrderCustomerIdOrderByCreatedAtDesc(2L))
            .thenReturn(List.of(canceled));

        PaymentResponse response = paymentService.findAll(2L).getFirst();

        assertEquals(40L, response.getId());
        assertEquals(30L, response.getOrderId());
        assertEquals(7000, response.getAmount());
        assertEquals(Payment.Status.CANCELED, response.getStatus());
    }

    @Test
    void canceledPaymentCannotBePaidAgain() {
        order.markCanceled();
        when(orderRepository.findWithLockById(30L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(30L)).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () ->
            paymentService.create(2L, 30L, cardRequest())
        );

        assertEquals(Order.Status.CANCELED, order.getStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void customerWithoutPaymentsGetsEmptyHistory() {
        when(paymentRepository.findAllByOrderCustomerIdOrderByCreatedAtDesc(3L))
            .thenReturn(List.of());

        assertTrue(paymentService.findAll(3L).isEmpty());
    }

    private CreatePaymentRequest cardRequest() {
        return fields(new CreatePaymentRequest(), "method", Payment.Method.CARD);
    }
}
