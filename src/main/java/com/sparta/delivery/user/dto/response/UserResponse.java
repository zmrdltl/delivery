package com.sparta.delivery.user.dto.response;

import com.sparta.delivery.user.entity.User;
import lombok.Getter;

@Getter
public class UserResponse {

    private final Long id;
    private final String loginId;
    private final User.Role role;

    public UserResponse(User user) {
        this.id = user.getId();
        this.loginId = user.getLoginId();
        this.role = user.getRole();
    }
}
