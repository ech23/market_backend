package com.example.ogani.service;

import com.example.ogani.entity.Order;
import com.example.ogani.entity.VNPayTransaction;
import com.example.ogani.model.payment.PaymentSession;
import java.util.Map;

public interface VNPayService {
    String createPaymentUrl(Order order, String ipAddress);
    Map<String, String> validatePaymentResponse(Map<String, String> response);
    void saveTransaction(Map<String, String> vnpayResponse, Order order);
    VNPayTransaction getTransactionByOrderId(String orderId);
    
    /**
     * Create a payment session and store order in Redis
     * @param order Order to process
     * @return PaymentSession object
     */
    PaymentSession createPaymentSession(Order order);
    
    /**
     * Get order by payment session ID
     * @param sessionId Payment session ID
     * @return Order object if found
     */
    Order getOrderBySessionId(String sessionId);
    
    /**
     * Complete payment session and remove order from Redis
     * @param sessionId Payment session ID
     * @param status Payment status (success or failed)
     */
    void completePaymentSession(String sessionId, String status);
}