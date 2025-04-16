package com.example.ogani.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "vnpay_transactions")
public class VNPayTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(unique = true)
    private String vnpTxnRef; // VNPay transaction reference

    private String vnpOrderInfo; // Order information
    
    private Long amount; // Amount in VND
    
    private String bankCode; // Bank code
    
    private String cardType; // Card type (ATM, VISA, MASTER...)
    
    private LocalDateTime paymentTime; // Payment time
    
    private String transactionStatus; // Transaction status from VNPay
    
    private String secureHash; // VNPay secure hash
    
    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order; // Reference to the order
    
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
} 