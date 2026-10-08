package com.sparta.delivery.payment.controller;

import com.sparta.delivery.payment.dto.request.CreatePaymentRequest;
import com.sparta.delivery.payment.dto.response.PaymentResponse;
import com.sparta.delivery.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "결제")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(
        summary = "주문 결제",
        description = "CUSTOMER 전용. 본인의 ORDERED 주문을 CARD로 결제하여 PAID로 변경합니다. 금액은 주문에서 계산하며 외부 결제 서비스는 호출하지 않습니다.",
        responses = {
            @ApiResponse(responseCode = "201", description = "결제 완료"),
            @ApiResponse(responseCode = "400", description = "입력·형식 오류"),
            @ApiResponse(responseCode = "404", description = "주문 없음"),
            @ApiResponse(responseCode = "409", description = "중복 결제·결제할 수 없는 주문 상태")
        }
    )
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

    @Operation(
        summary = "결제 내역",
        description = "CUSTOMER 전용. 본인 주문의 결제를 createdAt DESC로 조회합니다. 취소된 결제도 포함하며 결과가 없으면 빈 배열입니다."
    )
    @GetMapping("/payments")
    public ResponseEntity<List<PaymentResponse>> findAll(
        @AuthenticationPrincipal Long customerId
    ) {
        return ResponseEntity.ok(
            paymentService.findAll(customerId)
        );
    }
}