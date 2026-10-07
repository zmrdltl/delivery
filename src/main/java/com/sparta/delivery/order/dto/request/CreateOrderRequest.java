package com.sparta.delivery.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class CreateOrderRequest {

    @NotNull(message = "가게 ID는 필수입니다.")
    @Positive(message = "가게 ID는 양수여야 합니다.")
    private Long storeId;

    @NotBlank(message = "배달 주소는 필수입니다.")
    @Size(max = 255, message = "배달 주소는 255자 이하여야 합니다.")
    private String address;

    @NotEmpty(message = "주문할 메뉴를 하나 이상 선택해주세요.")
    @Valid
    private List<@NotNull(message = "주문 항목은 필수입니다.") OrderItemRequest> items;
}