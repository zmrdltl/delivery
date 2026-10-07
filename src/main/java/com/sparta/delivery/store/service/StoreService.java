package com.sparta.delivery.store.service;

import com.sparta.delivery.store.dto.request.CreateStoreRequest;
import com.sparta.delivery.store.dto.response.StoreResponse;
import com.sparta.delivery.store.entity.Store;
import com.sparta.delivery.store.repository.StoreRepository;
import com.sparta.delivery.user.entity.User;
import com.sparta.delivery.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    @Transactional
    public StoreResponse create(Long ownerId, CreateStoreRequest request) {
        User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "회원 정보를 확인할 수 없습니다."
            ));

        Store store = new Store(owner, request.getName());
        Store savedStore = storeRepository.save(store);

        return new StoreResponse(savedStore);
    }
}