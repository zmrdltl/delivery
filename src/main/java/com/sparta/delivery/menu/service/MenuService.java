package com.sparta.delivery.menu.service;

import com.sparta.delivery.menu.entity.Menu;
import com.sparta.delivery.menu.dto.request.CreateMenuRequest;
import com.sparta.delivery.menu.dto.response.MenuResponse;
import com.sparta.delivery.menu.repository.MenuRepository;
import com.sparta.delivery.store.entity.Store;
import com.sparta.delivery.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final StoreRepository storeRepository;

    @Transactional
    public MenuResponse create(Long ownerId, CreateMenuRequest request) {
        Store store = storeRepository.findById(request.getStoreId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "가게를 찾을 수 없습니다."
            ));

        if (!store.getOwner().getId().equals(ownerId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인 가게에만 메뉴를 등록할 수 있습니다."
            );
        }

        Menu menu = new Menu(
            store,
            request.getName(),
            request.getPrice(),
            request.getDescription()
        );

        Menu savedMenu = menuRepository.save(menu);
        return new MenuResponse(savedMenu);
    }
}