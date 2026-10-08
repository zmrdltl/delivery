package com.sparta.delivery.menu.repository;

import com.sparta.delivery.menu.entity.Menu;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    Page<Menu> findAllByDeletedFalse(Pageable pageable);

    Optional<Menu> findByIdAndDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Menu> findWithLockByIdAndDeletedFalse(Long id);
}
