package com.sparta.delivery.store.controller;

import com.sparta.delivery.store.dto.request.CreateStoreRequest;
import com.sparta.delivery.store.dto.response.StoreResponse;
import com.sparta.delivery.store.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @PostMapping
    public ResponseEntity<StoreResponse> create(
        @AuthenticationPrincipal Long ownerId,
        @Valid @RequestBody CreateStoreRequest request
    ) {
        StoreResponse response = storeService.create(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}