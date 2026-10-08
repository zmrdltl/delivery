package com.sparta.delivery.menu.controller;

import com.sparta.delivery.global.dto.response.PageResponse;
import com.sparta.delivery.menu.dto.request.CreateMenuRequest;
import com.sparta.delivery.menu.dto.request.UpdateMenuRequest;
import com.sparta.delivery.menu.dto.response.MenuResponse;
import com.sparta.delivery.menu.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "메뉴")
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(
        summary = "메뉴 등록",
        description = "OWNER 전용. 본인 가게에만 등록할 수 있습니다.",
        responses = {
            @ApiResponse(responseCode = "201", description = "메뉴 등록 완료"),
            @ApiResponse(responseCode = "400", description = "입력·형식 오류"),
            @ApiResponse(responseCode = "404", description = "가게 없음")
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<MenuResponse> create(
        @AuthenticationPrincipal Long ownerId,
        @Valid @RequestBody CreateMenuRequest request
    ) {
        MenuResponse response = menuService.create(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "메뉴 목록",
        description = "삭제되지 않은 메뉴를 createdAt DESC, id DESC로 조회합니다. page × size는 2,147,483,647 이하여야 합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "페이지 범위·형식 오류")
        }
    )
    @GetMapping
    public ResponseEntity<PageResponse<MenuResponse>> findAll(
        @Parameter(description = "0부터 시작", schema = @Schema(type = "integer", format = "int32", minimum = "0", defaultValue = "0"))
        @RequestParam(defaultValue = "0") int page,
        @Parameter(schema = @Schema(type = "integer", format = "int32", minimum = "1", maximum = "100", defaultValue = "10"))
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(menuService.findAll(page, size));
    }

    @Operation(
        summary = "메뉴 단건 조회",
        description = "삭제된 메뉴는 404입니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "ID 형식 오류"),
            @ApiResponse(responseCode = "404", description = "메뉴 없음 또는 삭제됨")
        }
    )
    @GetMapping("/{menuId}")
    public ResponseEntity<MenuResponse> findOne(
            @PathVariable Long menuId
    ) {
        return ResponseEntity.ok(menuService.findOne(menuId));
    }

    @Operation(
        summary = "메뉴 수정",
        description = "OWNER 전용. 본인 가게 메뉴만 수정합니다. PUT에서 description을 생략하면 null로 교체합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "입력·형식 오류"),
            @ApiResponse(responseCode = "404", description = "메뉴 없음 또는 삭제됨")
        }
    )
    @SecurityRequirement(name = "bearerAuth")
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

    @Operation(
        summary = "메뉴 삭제",
        description = "OWNER 전용. 본인 가게 메뉴를 삭제 표시합니다. 기존 주문 항목은 유지됩니다.",
        responses = {
            @ApiResponse(responseCode = "204", description = "삭제 완료, 응답 본문 없음", content = {}),
            @ApiResponse(responseCode = "400", description = "ID 형식 오류"),
            @ApiResponse(responseCode = "404", description = "메뉴 없음 또는 삭제됨")
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{menuId}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal Long ownerId,
        @PathVariable Long menuId
    ) {
        menuService.delete(ownerId, menuId);

        return ResponseEntity.noContent().build();
    }
}
