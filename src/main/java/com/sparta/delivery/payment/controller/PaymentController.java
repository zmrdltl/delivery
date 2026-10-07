package com.sparta.delivery.payment.controller;

import com.sparta.delivery.payment.dto.request.CreatePaymentRequest;
import com.sparta.delivery.payment.dto.response.PaymentResponse;
import com.sparta.delivery.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/orders/{orderId}/payments")
    public ResponseEntity<PaymentResponse> create(
        @AuthenticationPrincipal Long customerId,
        @PathVariable Long orderId,
        @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentResponse response = paymentService.create(
            customerId,
            orderId,
            request
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentResponse>> findAll(
        @AuthenticationPrincipal Long customerId
    ) {
        return ResponseEntity.ok(
            paymentService.findAll(customerId)
        );
    }
}