package com.sparta.delivery.menu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateMenuRequest {

    @NotNull(message = "가게 ID는 필수입니다.")
    @Positive(message = "가게 ID는 양수여야 합니다.")
    @Schema(example = "1")
    private Long storeId;

    @NotBlank(message = "메뉴 이름은 필수입니다.")
    @Size(max = 255, message = "메뉴 이름은 255자 이하여야 합니다.")
    @Schema(example = "김밥")
    private String name;

    @NotNull(message = "가격은 필수입니다.")
    @Positive(message = "가격은 1원 이상이어야 합니다.")
    @Schema(example = "3000")
    private Long price;

    @Schema(types = {"string", "null"}, example = "기본 김밥")
    private String description;
}
