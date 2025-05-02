package com.example.ogani.service;

import com.example.ogani.entity.Order;

import java.util.Optional;

public interface RedisService {
    
    /**
     * Store order in Redis for VNPay payment processing
     * @param order Order to store
     * @param paymentSessionId unique payment session ID
     * @return the payment session ID
     */
    String storeOrderForPayment(Order order, String paymentSessionId);
    
    /**
     * Retrieve order from Redis by payment session ID
     * @param paymentSessionId payment session ID
     * @return Optional containing the order if found
     */
    Optional<Order> getOrderByPaymentSessionId(String paymentSessionId);
    
    /**
     * Remove order from Redis after payment is completed or failed
     * @param paymentSessionId payment session ID
     */
    void removeOrder(String paymentSessionId);
}