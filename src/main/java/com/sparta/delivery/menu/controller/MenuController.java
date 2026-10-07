package com.sparta.delivery.menu.controller;

import com.sparta.delivery.menu.dto.request.CreateMenuRequest;
import com.sparta.delivery.menu.dto.response.MenuResponse;
import com.sparta.delivery.menu.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @PostMapping
    public ResponseEntity<MenuResponse> create(
        @AuthenticationPrincipal Long ownerId,
        @Valid @RequestBody CreateMenuRequest request
    ) {
        MenuResponse response = menuService.create(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}