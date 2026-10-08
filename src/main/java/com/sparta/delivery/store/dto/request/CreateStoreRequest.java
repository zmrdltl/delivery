package com.sparta.delivery.store.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateStoreRequest {

    @NotBlank(message = "가게 이름은 필수입니다.")
    @Size(max = 255, message = "가게 이름은 255자 이하여야 합니다.")
    @Schema(example = "김밥집")
    private String name;
}
