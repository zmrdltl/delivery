package com.sparta.delivery.menu.repository;

import com.sparta.delivery.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findAllByDeletedFalse();

    Optional<Menu> findByIdAndDeletedFalse(Long id);
}