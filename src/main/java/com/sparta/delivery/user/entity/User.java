package com.sparta.delivery.user.entity;

import com.sparta.delivery.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String loginId;

    @Column(nullable = false)
    private String password;

    public enum Role {
        CUSTOMER,
        OWNER
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public User(String loginId, String encodedPassword, Role role) {
        this.loginId = loginId;
        this.password = encodedPassword;
        this.role = role;
    }
}
