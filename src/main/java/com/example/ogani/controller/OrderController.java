package com.example.ogani.controller;

import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.ogani.entity.Order;
import com.example.ogani.entity.OrderStatus;
import com.example.ogani.exception.InsufficientStockException;
import com.example.ogani.exception.NotFoundException;
import com.example.ogani.model.request.CreateOrderRequest;
import com.example.ogani.model.response.MessageResponse;
import com.example.ogani.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/order")
//@CrossOrigin(origins = "*",maxAge = 3600)
public class OrderController {
    @Autowired
    private OrderService orderService;


    @GetMapping("/")
    @Operation(summary="Lấy ra danh sách đặt hàng")
    public ResponseEntity<List<Order>> getList(){
        List<Order> list = orderService.getList();

        return ResponseEntity.ok(list);
    }

    @GetMapping("/user")
    @Operation(summary="Lấy ra danh sách đặt hàng của người dùng bằng username")
    public ResponseEntity<List<Order>> getListByUser(@RequestParam("username") String username){
        List<Order> list = orderService.getOrderByUser(username);

        return ResponseEntity.ok(list);
    }

    @GetMapping("/status")
    @Operation(summary="Lấy danh sách đơn hàng theo trạng thái")
    public ResponseEntity<?> getOrdersByStatus(@RequestParam("status") String statusStr) {
        try {
            OrderStatus status = OrderStatus.valueOf(statusStr);
            List<Order> orders = orderService.getOrdersByStatus(status);
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Trạng thái đơn hàng không hợp lệ: " + statusStr));
        }
    }
    
    @GetMapping("/user/status")
    @Operation(summary="Lấy danh sách đơn hàng theo trạng thái và username")
    public ResponseEntity<?> getOrdersByStatusAndUser(
            @RequestParam("status") String statusStr,
            @RequestParam("username") String username) {
        try {
            OrderStatus status = OrderStatus.valueOf(statusStr);
            List<Order> orders = orderService.getOrdersByStatusAndUser(status, username);
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Trạng thái đơn hàng không hợp lệ: " + statusStr));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/create")
    @Operation(summary="Đặt hàng sản phẩm")
    public ResponseEntity<?> placeOrder(@RequestBody CreateOrderRequest request){
        try {
            Order order = orderService.placeOrder(request);
            return ResponseEntity.ok(order);
        } catch (InsufficientStockException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Có lỗi xảy ra: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    @Operation(summary="Lấy thông tin đơn hàng theo ID")
    public ResponseEntity<Order> getOrderById(@PathVariable long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }
    
    @PostMapping("/{id}/update-payment")
    @Operation(summary="Cập nhật trạng thái thanh toán")
    public ResponseEntity<Order> updatePaymentStatus(
            @PathVariable long id,
            @RequestParam("status") String status,
            @RequestParam("paymentMethod") String paymentMethod) {
        Order order = orderService.updateOrderPaymentStatus(id, status, paymentMethod);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/update-status")
    @Operation(summary="Cập nhật trạng thái đơn hàng")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable long id, 
            @RequestParam("status") String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status);
            Order order = orderService.updateOrderStatus(id, orderStatus);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Trạng thái đơn hàng không hợp lệ: " + status));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Có lỗi xảy ra: " + e.getMessage()));
        }
    }

    @GetMapping("/statuses")
    @Operation(summary="Lấy danh sách các trạng thái đơn hàng")
    public ResponseEntity<Map<String, String>> getOrderStatuses() {
        Map<String, String> statuses = Arrays.stream(OrderStatus.values())
                .collect(Collectors.toMap(
                        status -> status.name(),
                        status -> status.getDisplayName()
                ));
        return ResponseEntity.ok(statuses);
    }
}
