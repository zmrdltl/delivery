package com.sparta.delivery.store.repository;

import com.sparta.delivery.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
}