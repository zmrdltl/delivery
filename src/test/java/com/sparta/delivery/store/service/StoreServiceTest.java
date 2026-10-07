package com.sparta.delivery.store.service;

import com.sparta.delivery.store.dto.request.CreateStoreRequest;
import com.sparta.delivery.store.dto.response.StoreResponse;
import com.sparta.delivery.store.entity.Store;
import com.sparta.delivery.store.repository.StoreRepository;
import com.sparta.delivery.user.entity.User;
import com.sparta.delivery.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static com.sparta.delivery.support.ServiceFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;
    @Mock
    private UserRepository userRepository;

    private StoreService storeService;

    @BeforeEach
    void setUp() {
        storeService = new StoreService(storeRepository, userRepository);
    }

    @Test
    void createConnectsStoreToAuthenticatedOwner() {
        User owner = user(1L, User.Role.OWNER);
        CreateStoreRequest request = fields(new CreateStoreRequest(), "name", "김밥집");
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation ->
            fields(invocation.getArgument(0), "id", 10L)
        );

        StoreResponse response = storeService.create(1L, request);

        assertEquals(10L, response.getId());
        assertEquals(1L, response.getOwnerId());
        assertEquals("김밥집", response.getName());
    }

    @Test
    void missingOwnerDoesNotCreateStore() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.UNAUTHORIZED, () ->
            storeService.create(1L, new CreateStoreRequest())
        );

        verifyNoInteractions(storeRepository);
    }
}
