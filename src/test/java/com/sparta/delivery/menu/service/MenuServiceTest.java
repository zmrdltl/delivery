package com.sparta.delivery.menu.service;

import com.sparta.delivery.menu.dto.request.CreateMenuRequest;
import com.sparta.delivery.menu.dto.request.UpdateMenuRequest;
import com.sparta.delivery.menu.dto.response.MenuResponse;
import com.sparta.delivery.menu.entity.Menu;
import com.sparta.delivery.menu.repository.MenuRepository;
import com.sparta.delivery.store.entity.Store;
import com.sparta.delivery.store.repository.StoreRepository;
import com.sparta.delivery.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static com.sparta.delivery.support.ServiceFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;
    @Mock
    private StoreRepository storeRepository;

    private MenuService menuService;
    private Store store;
    private Menu menu;

    @BeforeEach
    void setUp() {
        menuService = new MenuService(menuRepository, storeRepository);
        store = store(10L, user(1L, User.Role.OWNER));
        menu = menu(20L, store, 3000);
    }

    @Test
    void createSavesMenuInOwnersStore() {
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
        when(menuRepository.save(any(Menu.class))).thenAnswer(invocation ->
            fields(invocation.getArgument(0), "id", 20L)
        );

        MenuResponse response = menuService.create(1L, createRequest());

        assertEquals(10L, response.getStoreId());
        assertEquals("김밥", response.getName());
        assertEquals(3000, response.getPrice());
    }

    @Test
    void otherOwnerCannotCreateMenuInStore() {
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));

        assertStatus(HttpStatus.FORBIDDEN, () -> menuService.create(3L, createRequest()));

        verify(menuRepository, never()).save(any(Menu.class));
    }

    @Test
    void missingStoreDoesNotSaveMenu() {
        when(storeRepository.findById(10L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> menuService.create(1L, createRequest()));

        verifyNoInteractions(menuRepository);
    }

    @Test
    void listMapsActiveMenusToResponses() {
        when(menuRepository.findAllByDeletedFalse()).thenReturn(List.of(menu));

        List<MenuResponse> response = menuService.findAll();

        assertEquals(1, response.size());
        assertEquals(20L, response.getFirst().getId());
        assertEquals(10L, response.getFirst().getStoreId());
    }

    @Test
    void findOneReturnsActiveMenu() {
        when(menuRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(menu));

        assertEquals(3000, menuService.findOne(20L).getPrice());
    }

    @Test
    void missingOrDeletedMenuReturnsNotFound() {
        when(menuRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> menuService.findOne(20L));
    }

    @Test
    void missingMenuCannotBeUpdated() {
        when(menuRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () ->
            menuService.update(1L, 20L, updateRequest())
        );
    }

    @Test
    void missingMenuCannotBeDeleted() {
        when(menuRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> menuService.delete(1L, 20L));
    }

    @Test
    void ownUpdateReplacesEditableFieldsIncludingOptionalDescription() {
        when(menuRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(menu));

        MenuResponse response = menuService.update(1L, 20L, updateRequest());

        assertEquals("떡", menu.getName());
        assertEquals(5000, menu.getPrice());
        assertNull(menu.getDescription());
        assertEquals("떡", response.getName());
        assertEquals(10L, response.getStoreId());
    }

    @Test
    void otherOwnerUpdateLeavesMenuUnchanged() {
        when(menuRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(menu));

        assertStatus(HttpStatus.FORBIDDEN, () ->
            menuService.update(3L, 20L, updateRequest())
        );

        assertEquals("메뉴20", menu.getName());
        assertEquals(3000, menu.getPrice());
        assertEquals("설명", menu.getDescription());
    }

    @Test
    void ownDeletionMarksMenuWithoutDeletingRow() {
        when(menuRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(menu));

        menuService.delete(1L, 20L);

        assertTrue(menu.isDeleted());
        verify(menuRepository, never()).delete(any(Menu.class));
        verify(menuRepository, never()).deleteById(any());
    }

    @Test
    void otherOwnerDeletionLeavesMenuActive() {
        when(menuRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(menu));

        assertStatus(HttpStatus.FORBIDDEN, () -> menuService.delete(3L, 20L));

        assertFalse(menu.isDeleted());
    }

    private CreateMenuRequest createRequest() {
        return fields(
            new CreateMenuRequest(), "storeId", 10L,
            "name", "김밥", "price", 3000L, "description", "설명"
        );
    }

    private UpdateMenuRequest updateRequest() {
        return fields(new UpdateMenuRequest(), "name", "떡", "price", 5000L);
    }
}
