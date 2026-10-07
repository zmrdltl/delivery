package com.sparta.delivery.menu;

import com.sparta.delivery.global.entity.BaseEntity;
import com.sparta.delivery.store.entity.Store;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "menus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private long price;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private boolean deleted = false;

    public Menu(Store store, String name, long price, String description) {
        this.store = store;
        this.name = name;
        this.price = price;
        this.description = description;
    }

    public void update(String name, long price, String description) {
        this.name = name;
        this.price = price;
        this.description = description;
    }

    public void delete() {
        this.deleted = true;
    }
}