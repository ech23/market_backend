package com.example.ogani.service;

import java.util.List;

import com.example.ogani.entity.Order;
import com.example.ogani.model.request.CreateOrderRequest;

public interface OrderService {
    
    Order placeOrder(CreateOrderRequest request);

    List<Order> getList();
    
    List<Order> getOrderByUser(String username);
    
    Order getOrderById(String id);
    
    Order updateOrderPaymentStatus(String id, String status, String paymentMethod);
}
