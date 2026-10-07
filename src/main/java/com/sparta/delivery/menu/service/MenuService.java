package com.sparta.delivery.menu.service;

import com.sparta.delivery.menu.dto.request.UpdateMenuRequest;
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

import java.util.List;

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

    @Transactional(readOnly = true)
    public List<MenuResponse> findAll() {
        return menuRepository.findAllByDeletedFalse()
            .stream()
            .map(MenuResponse::new)
            .toList();
    }

    @Transactional(readOnly = true)
    public MenuResponse findOne(Long menuId) {
        Menu menu = menuRepository.findByIdAndDeletedFalse(menuId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "메뉴를 찾을 수 없습니다."
            ));

        return new MenuResponse(menu);
    }

    @Transactional
    public MenuResponse update(
        Long ownerId,
        Long menuId,
        UpdateMenuRequest request
    ) {
        Menu menu = menuRepository.findByIdAndDeletedFalse(menuId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "메뉴를 찾을 수 없습니다."
            ));

        if (!menu.getStore().getOwner().getId().equals(ownerId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인 가게의 메뉴만 수정할 수 있습니다."
            );
        }

        menu.update(
            request.getName(),
            request.getPrice(),
            request.getDescription()
        );

        return new MenuResponse(menu);
    }
}