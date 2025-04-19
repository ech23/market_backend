package com.example.ogani.service;

import java.util.List;

import com.example.ogani.entity.Order;
import com.example.ogani.model.request.CreateOrderRequest;

public interface OrderService {
    
    Order placeOrder(CreateOrderRequest request);

    List<Order> getList();
    
    List<Order> getOrderByUser(String username);
    
    Order getOrderById(long id);
    
    Order updateOrderPaymentStatus(long id, String status, String paymentMethod);
}
