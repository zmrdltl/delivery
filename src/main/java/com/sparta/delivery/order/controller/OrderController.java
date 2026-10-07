package com.sparta.delivery.order.controller;

import com.sparta.delivery.order.dto.request.CreateOrderRequest;
import com.sparta.delivery.order.dto.response.OrderResponse;
import com.sparta.delivery.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> create(
        @AuthenticationPrincipal Long customerId,
        @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderResponse response = orderService.create(
            customerId,
            request
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> findAll(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(orderService.findAll(userId));
    }
}