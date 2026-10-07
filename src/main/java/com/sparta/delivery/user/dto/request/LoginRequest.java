package com.sparta.delivery.user.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

@Getter
public class LoginRequest {

    @NotBlank(message = "아이디는 필수입니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    @AssertTrue(message = "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.")
    public boolean isPasswordWithinByteLimit() {
        return password == null
            || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}