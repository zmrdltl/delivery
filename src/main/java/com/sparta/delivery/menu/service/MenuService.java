package com.sparta.delivery.menu.service;

import com.sparta.delivery.global.dto.response.PageResponse;
import com.sparta.delivery.menu.dto.request.UpdateMenuRequest;
import com.sparta.delivery.menu.entity.Menu;
import com.sparta.delivery.menu.dto.request.CreateMenuRequest;
import com.sparta.delivery.menu.dto.response.MenuResponse;
import com.sparta.delivery.menu.repository.MenuRepository;
import com.sparta.delivery.store.entity.Store;
import com.sparta.delivery.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    @Transactional(readOnly = true)
    public PageResponse<MenuResponse> findAll(int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "페이지 번호는 0 이상이어야 합니다."
            );
        }

        if (size < 1 || size > 100) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "페이지 크기는 1부터 100까지 가능합니다."
            );
        }

        if ((long) page * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "페이지 조회 범위가 처리 가능한 한도를 초과했습니다."
            );
        }

        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );
        Page<MenuResponse> menus = menuRepository.findAllByDeletedFalse(pageable)
            .map(MenuResponse::new);

        return new PageResponse<>(menus);
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

    @Transactional
    public void delete(Long ownerId, Long menuId) {
        Menu menu = menuRepository.findByIdAndDeletedFalse(menuId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "메뉴를 찾을 수 없습니다."
            ));

        if(!menu.getStore().getOwner().getId().equals(ownerId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인 가게의 메뉴만 삭제할 수 있습니다."
            );
        }

        menu.delete();
    }
}