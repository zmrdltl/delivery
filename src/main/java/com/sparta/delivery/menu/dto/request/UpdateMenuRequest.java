package com.sparta.delivery.menu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateMenuRequest {

    @NotBlank(message = "메뉴 이름은 필수입니다.")
    @Size(max = 255, message = "메뉴 이름은 255자 이하여야 합니다.")
    private String name;

    @NotNull(message = "가격은 필수입니다.")
    @Positive(message = "가격은 1원 이상이어야 합니다.")
    private Long price;

    private String description;
}