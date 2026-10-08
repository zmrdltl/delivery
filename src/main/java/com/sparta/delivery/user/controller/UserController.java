package com.sparta.delivery.user.controller;

import com.sparta.delivery.user.dto.request.LoginRequest;
import com.sparta.delivery.user.dto.request.SignupRequest;
import com.sparta.delivery.user.dto.response.LoginResponse;
import com.sparta.delivery.user.dto.response.UserResponse;
import com.sparta.delivery.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
        summary = "회원가입",
        description = "OWNER 또는 CUSTOMER로 가입합니다. 아이디가 중복되면 409입니다.",
        responses = {
            @ApiResponse(responseCode = "201", description = "가입 완료"),
            @ApiResponse(responseCode = "400", description = "입력·형식 오류"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 아이디")
        }
    )
    @PostMapping
    public ResponseEntity<UserResponse> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        UserResponse response = userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "로그인",
        description = "발급된 token을 Authorize에 입력합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "입력·형식 오류"),
            @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 불일치")
        }
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login (
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }
}
