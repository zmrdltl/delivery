package com.sparta.delivery.menu.dto.response;

import com.sparta.delivery.menu.Menu;
import lombok.Getter;

@Getter
public class MenuResponse {

    private final Long id;
    private final Long storeId;
    private final String name;
    private final long price;
    private final String description;

    public MenuResponse(Menu menu) {
        this.id = menu.getId();
        this.storeId = menu.getStore().getId();
        this.name = menu.getName();
        this.price = menu.getPrice();
        this.description = menu.getDescription();
    }
}