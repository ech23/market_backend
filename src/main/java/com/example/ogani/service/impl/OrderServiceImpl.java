package com.example.ogani.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.ogani.entity.Order;
import com.example.ogani.entity.OrderDetail;
import com.example.ogani.entity.User;
import com.example.ogani.exception.NotFoundException;
import com.example.ogani.model.request.CreateOrderDetailRequest;
import com.example.ogani.model.request.CreateOrderRequest;
import com.example.ogani.repository.OrderDetailRepository;
import com.example.ogani.repository.OrderRepository;
import com.example.ogani.repository.UserRepository;
import com.example.ogani.service.OrderService;

@Service
public class OrderServiceImpl implements OrderService {
    
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Order placeOrder(CreateOrderRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new NotFoundException("Not Found User With Username:" + request.getUsername()));

        Order order = new Order();
        order.setFirstname(request.getFirstname());
        order.setLastname(request.getLastname());
        order.setCountry(request.getCountry());
        order.setAddress(request.getAddress());
        order.setTown(request.getTown());
        order.setState(request.getState());
        order.setPostCode(request.getPostCode());
        order.setEmail(request.getEmail());
        order.setPhone(request.getPhone());
        order.setNote(request.getNote());

        long totalPrice = 0;
        order = orderRepository.save(order);  // Save lần 1 để có ID cho OrderDetail

        for (CreateOrderDetailRequest rq : request.getOrderDetails()) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setName(rq.getName());
            orderDetail.setPrice(rq.getPrice());
            orderDetail.setQuantity(rq.getQuantity());
            orderDetail.setSubTotal(rq.getPrice() * rq.getQuantity());
            orderDetail.setOrder(order);
            totalPrice += orderDetail.getSubTotal();
            orderDetailRepository.save(orderDetail);
        }

        order.setTotalPrice(totalPrice);
        order.setUser(user);

        if (request.getPaymentMethod() != null && !request.getPaymentMethod().isEmpty()) {
            order.setPaymentMethod(request.getPaymentMethod());
        }

        return orderRepository.save(order);  // Save lần 2 cập nhật totalPrice và user
    }

    @Override
    public List<Order> getList() {
        return orderRepository.findAll(Sort.by("id").descending());
    }

    @Override
    public List<Order> getOrderByUser(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("Not Found User With Username:" + username));

        List<Order> orders = orderRepository.getOrderByUser(user.getId());
        return orders;  
    }

    @Override
    public Order getOrderById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found with ID: " + id));
    }

    @Override
    public Order updateOrderPaymentStatus(String id, String status, String paymentMethod) {
        Order order = getOrderById(id);
        order.setPaymentStatus(status);
        order.setPaymentMethod(paymentMethod);
        return orderRepository.save(order);
    }
}
