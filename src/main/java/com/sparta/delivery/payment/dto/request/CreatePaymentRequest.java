package com.sparta.delivery.payment.dto.request;

import com.sparta.delivery.payment.entity.Payment;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CreatePaymentRequest {

    @NotNull(message = "결제 수단은 필수입니다.")
    private Payment.Method method;
}