package com.sparta.delivery.menu.repository;

import com.sparta.delivery.menu.entity.Menu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    Page<Menu> findAllByDeletedFalse(Pageable pageable);

    Optional<Menu> findByIdAndDeletedFalse(Long id);
}