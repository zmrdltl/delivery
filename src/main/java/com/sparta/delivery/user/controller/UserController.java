package com.sparta.delivery.user.controller;

import com.sparta.delivery.user.dto.request.SignupRequest;
import com.sparta.delivery.user.dto.response.UserResponse;
import com.sparta.delivery.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(
            @Valid @RequestBody SignupRequest request
    ) {
        return userService.signup(request);
    }
}
