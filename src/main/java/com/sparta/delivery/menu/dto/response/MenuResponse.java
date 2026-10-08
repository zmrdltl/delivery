package com.sparta.delivery.menu.dto.response;

import com.sparta.delivery.menu.entity.Menu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class MenuResponse {

    private final Long id;
    private final Long storeId;
    private final String name;
    private final long price;
    @Schema(types = {"string", "null"})
    private final String description;

    public MenuResponse(Menu menu) {
        this.id = menu.getId();
        this.storeId = menu.getStore().getId();
        this.name = menu.getName();
        this.price = menu.getPrice();
        this.description = menu.getDescription();
    }
}
