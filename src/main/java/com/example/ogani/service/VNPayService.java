package com.example.ogani.service;

import com.example.ogani.entity.Order;
import java.util.Map;

public interface VNPayService {
    String createPaymentUrl(Order order, String ipAddress);
    Map<String, String> validatePaymentResponse(Map<String, String> response);
    void saveTransaction(Map<String, String> vnpayResponse, Order order);
} 