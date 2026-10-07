package com.sparta.delivery.menu.controller;

import com.sparta.delivery.menu.dto.request.CreateMenuRequest;
import com.sparta.delivery.menu.dto.response.MenuResponse;
import com.sparta.delivery.menu.repository.MenuRepository;
import com.sparta.delivery.menu.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final MenuRepository menuRepository;

    @PostMapping
    public ResponseEntity<MenuResponse> create(
        @AuthenticationPrincipal Long ownerId,
        @Valid @RequestBody CreateMenuRequest request
    ) {
        MenuResponse response = menuService.create(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<MenuResponse>> findAll() {
        return ResponseEntity.ok(menuService.findAll());
    }

    @GetMapping("/{menuId}")
    public ResponseEntity<MenuResponse> findOne(
            @PathVariable Long menuId
    ) {
        return ResponseEntity.ok(menuService.findOne(menuId));
    }
}