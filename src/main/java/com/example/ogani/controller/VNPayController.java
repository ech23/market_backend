package com.example.ogani.controller;

import com.example.ogani.entity.Order;
import com.example.ogani.service.OrderService;
import com.example.ogani.service.VNPayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/vnpay")
@CrossOrigin(origins = "http://localhost:4200")
public class VNPayController {

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private OrderService orderService;

    @PostMapping("/create-payment/{orderId}")
    public ResponseEntity<?> createPaymentUrl(@PathVariable String orderId, HttpServletRequest request) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.badRequest().body("Order not found");
        }

        String clientIp = request.getRemoteAddr();  // lấy IP client
        String vnpayUrl = vnPayService.createPaymentUrl(order, clientIp);

        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", vnpayUrl);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/payment-callback")
    public ResponseEntity<?> paymentCallback(@RequestParam Map<String, String> queryParams) {
        Map<String, String> response = vnPayService.validatePaymentResponse(queryParams);
        
        if ("success".equals(response.get("status"))) {
            String vnpOrderInfo = queryParams.get("vnp_OrderInfo");
            String orderId = vnpOrderInfo.substring(vnpOrderInfo.lastIndexOf(": ") + 2);
            Order order = orderService.getOrderById(orderId);
            
            if (order != null) {
                // Save VNPay transaction
                vnPayService.saveTransaction(queryParams, order);
                
                // Update order payment status to PAID if transaction successful
                if ("00".equals(queryParams.get("vnp_ResponseCode"))) {
                    orderService.updateOrderPaymentStatus(order.getId(), "PAID", "VNPAY");
                    response.put("orderStatus", "PAID");
                } else {
                    orderService.updateOrderPaymentStatus(order.getId(), "CANCELLED", "VNPAY");
                    response.put("orderStatus", "CANCELLED");
                }
                
                return ResponseEntity.ok(response);
            }
        }
        
        return ResponseEntity.badRequest().body(response);
    }
    
    @GetMapping("/payment-status/{orderId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String orderId) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.badRequest().body("Order not found");
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("orderId", String.valueOf(order.getId()));
        response.put("paymentMethod", order.getPaymentMethod());
        response.put("paymentStatus", order.getPaymentStatus());
        
        return ResponseEntity.ok(response);
    }
} 