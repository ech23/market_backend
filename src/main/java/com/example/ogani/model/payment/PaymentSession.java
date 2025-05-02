package com.example.ogani.model.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSession implements Serializable {
    private String sessionId;
    private Long orderId;
    private String paymentMethod;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}