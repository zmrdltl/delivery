package com.sparta.delivery.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

@Getter
public class LoginRequest {

    @NotBlank(message = "아이디는 필수입니다.")
    @Schema(example = "owner1")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Schema(description = "UTF-8 72바이트 이하", format = "password", example = "example-password")
    private String password;

    @Schema(hidden = true)
    @AssertTrue(message = "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.")
    public boolean isPasswordWithinByteLimit() {
        return password == null
            || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
