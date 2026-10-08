package com.sparta.delivery.store.controller;

import com.sparta.delivery.store.dto.request.CreateStoreRequest;
import com.sparta.delivery.store.dto.response.StoreResponse;
import com.sparta.delivery.store.service.StoreService;
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

@Tag(name = "가게")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @Operation(
        summary = "가게 생성",
        description = "OWNER 전용. 인증된 사장의 가게를 생성합니다.",
        responses = {
            @ApiResponse(responseCode = "201", description = "가게 생성 완료"),
            @ApiResponse(responseCode = "400", description = "입력·형식 오류")
        }
    )
    @PostMapping
    public ResponseEntity<StoreResponse> create(
        @AuthenticationPrincipal Long ownerId,
        @Valid @RequestBody CreateStoreRequest request
    ) {
        StoreResponse response = storeService.create(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}