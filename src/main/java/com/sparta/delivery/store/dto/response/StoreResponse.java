package com.sparta.delivery.store.dto.response;

import com.sparta.delivery.store.entity.Store;
import lombok.Getter;

@Getter
public class StoreResponse {

    private final Long id;
    private final String name;
    private final Long ownerId;

    public StoreResponse(Store store) {
        this.id = store.getId();
        this.name = store.getName();
        this.ownerId = store.getOwner().getId();
    }
}