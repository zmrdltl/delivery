package com.sparta.delivery.order.controller;

import com.sparta.delivery.order.dto.request.CreateOrderRequest;
import com.sparta.delivery.order.dto.response.OrderResponse;
import com.sparta.delivery.order.service.OrderService;
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

@Tag(name = "주문")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(
        summary = "주문 생성",
        description = "CUSTOMER 전용. 활성 상태인 같은 가게 메뉴만 주문합니다. 메뉴 이름·단가를 주문 당시 값으로 보관합니다. 같은 menuId의 항목도 각각 저장합니다.",
        responses = {
            @ApiResponse(responseCode = "201", description = "ORDERED 주문 생성"),
            @ApiResponse(responseCode = "400", description = "입력 오류·다른 가게 메뉴·금액 범위 초과"),
            @ApiResponse(responseCode = "404", description = "가게 또는 활성 메뉴 없음")
        }
    )
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

    @Operation(
        summary = "주문 목록",
        description = "CUSTOMER는 본인 주문, OWNER는 본인 가게 주문을 조회합니다. 결과가 없으면 빈 배열입니다."
    )
    @GetMapping
    public ResponseEntity<List<OrderResponse>> findAll(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(orderService.findAll(userId));
    }

    @Operation(
        summary = "주문 수락",
        description = "OWNER 전용. 본인 가게의 PAID 주문을 ACCEPTED로 변경합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "ID 형식 오류"),
            @ApiResponse(responseCode = "404", description = "주문 없음"),
            @ApiResponse(responseCode = "409", description = "수락할 수 없는 상태")
        }
    )
    @PatchMapping("/{orderId}/accept")
    public ResponseEntity<OrderResponse> accept(
        @AuthenticationPrincipal Long ownerId,
        @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(
            orderService.accept(ownerId, orderId)
        );
    }

    @Operation(
        summary = "배달 완료",
        description = "OWNER 전용. 본인 가게의 ACCEPTED 주문을 DELIVERED로 변경합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "ID 형식 오류"),
            @ApiResponse(responseCode = "404", description = "주문 없음"),
            @ApiResponse(responseCode = "409", description = "배달 완료로 변경할 수 없는 상태")
        }
    )
    @PatchMapping("/{orderId}/deliver")
    public ResponseEntity<OrderResponse> deliver(
        @AuthenticationPrincipal Long ownerId,
        @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(
            orderService.deliver(ownerId, orderId)
        );
    }

    @Operation(
        summary = "주문 거절",
        description = "OWNER 전용. 본인 가게의 ORDERED 또는 PAID 주문을 REJECTED로 변경합니다. 시간 제한은 없으며 PAID의 결제도 CANCELED로 변경합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "ID 형식 오류"),
            @ApiResponse(responseCode = "404", description = "주문 없음"),
            @ApiResponse(responseCode = "409", description = "거절할 수 없는 상태·결제 기록 불일치")
        }
    )
    @PatchMapping("/{orderId}/reject")
    public ResponseEntity<OrderResponse> reject(
        @AuthenticationPrincipal Long ownerId,
        @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(
            orderService.reject(ownerId, orderId)
        );
    }

    @Operation(
        summary = "주문 취소",
        description = "CUSTOMER 전용. 본인의 ORDERED 또는 PAID 주문을 생성 후 정확히 5분까지 CANCELED로 변경합니다. PAID의 결제도 CANCELED로 변경합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "ID 형식 오류"),
            @ApiResponse(responseCode = "404", description = "주문 없음"),
            @ApiResponse(responseCode = "409", description = "취소할 수 없는 상태·기한 초과·결제 기록 불일치")
        }
    )
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancel(
        @AuthenticationPrincipal Long customerId,
        @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(
            orderService.cancel(customerId, orderId)
        );
    }

    @Operation(
        summary = "주문 단건 조회",
        description = "CUSTOMER는 본인 주문, OWNER는 본인 가게 주문만 조회합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "ID 형식 오류"),
            @ApiResponse(responseCode = "404", description = "주문 없음")
        }
    )
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> findOne(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(
            orderService.findOne(userId, orderId)
        );
    }
}