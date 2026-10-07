package com.sparta.delivery.menu.controller;

import com.sparta.delivery.global.dto.response.PageResponse;
import com.sparta.delivery.menu.dto.request.CreateMenuRequest;
import com.sparta.delivery.menu.dto.request.UpdateMenuRequest;
import com.sparta.delivery.menu.dto.response.MenuResponse;
import com.sparta.delivery.menu.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public ResponseEntity<PageResponse<MenuResponse>> findAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(menuService.findAll(page, size));
    }

    @GetMapping("/{menuId}")
    public ResponseEntity<MenuResponse> findOne(
            @PathVariable Long menuId
    ) {
        return ResponseEntity.ok(menuService.findOne(menuId));
    }

    @PutMapping("/{menuId}")
    public ResponseEntity<MenuResponse> update(
        @AuthenticationPrincipal Long ownerId,
        @PathVariable Long menuId,
        @Valid @RequestBody UpdateMenuRequest request
    ) {
        MenuResponse response = menuService.update(
            ownerId, menuId, request
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{menuId}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal Long ownerId,
        @PathVariable Long menuId
    ) {
        menuService.delete(ownerId, menuId);

        return ResponseEntity.noContent().build();
    }
}