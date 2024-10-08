package com.sorisonsoon.payment.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentDTO {
    private Long paymentId;
    private Long payerId;
    private int amount;
    private LocalDateTime payedAt;
}