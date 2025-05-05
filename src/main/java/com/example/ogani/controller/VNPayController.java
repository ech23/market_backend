package com.example.ogani.controller;

import com.example.ogani.entity.Order;
import com.example.ogani.exception.NotFoundException;
import com.example.ogani.model.payment.PaymentSession;
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
//@CrossOrigin(origins = "http://localhost:4200")
public class VNPayController {

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private OrderService orderService;

    @PostMapping("/create-payment/{orderId}")
    public ResponseEntity<?> createPaymentUrl(@PathVariable long orderId, HttpServletRequest request) {
        try {
            // Get order by ID
            Order order = orderService.getOrderById(orderId);
            
            // Create payment session and store order in Redis
            PaymentSession session = vnPayService.createPaymentSession(order);
            
            // Get client IP
            String clientIp = request.getRemoteAddr();
            
            // Create VNPay payment URL
            String vnpayUrl = vnPayService.createPaymentUrl(order, clientIp);
            
            // Create response
            Map<String, String> response = new HashMap<>();
            response.put("paymentUrl", vnpayUrl);
            response.put("sessionId", session.getSessionId());

            
            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating payment: " + e.getMessage());
        }
    }

    @GetMapping("/payment-callback")
    public ResponseEntity<?> paymentCallback(@RequestParam Map<String, String> queryParams) {
        try {
            Map<String, String> response = vnPayService.validatePaymentResponse(queryParams);
            
            if ("success".equals(response.get("status"))) {
                String vnpOrderInfo = queryParams.get("vnp_OrderInfo");
                String orderId = vnpOrderInfo.substring(vnpOrderInfo.lastIndexOf(": ") + 2);
                
                // Get order from database
                Order order = orderService.getOrderById(Long.parseLong(orderId));
                
                if (order != null) {
                    // Save VNPay transaction
                    vnPayService.saveTransaction(queryParams, order);
                    
                    // Update order payment status
                    if ("00".equals(queryParams.get("vnp_ResponseCode"))) {
                        orderService.updateOrderPaymentStatus(order.getId(), "PAID", "VNPAY");
                        response.put("orderStatus", "PAID");
                    } else {
                        orderService.updateOrderPaymentStatus(order.getId(), "CANCELLED", "VNPAY");
                        response.put("orderStatus", "CANCELLED");
                    }
                    
                    // Add sessionId to response for client to use
                    response.put("orderId", String.valueOf(order.getId()));
                    return ResponseEntity.ok(response);
                }
            }
            
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/payment-status/{orderId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable long orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            
            Map<String, String> response = new HashMap<>();
            response.put("orderId", String.valueOf(order.getId()));
            response.put("paymentMethod", order.getPaymentMethod());
            response.put("paymentStatus", order.getPaymentStatus());
            
            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.badRequest().body("Order not found");
        }
    }
    
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getPaymentSession(@PathVariable String sessionId) {
        try {
            Order order = vnPayService.getOrderBySessionId(sessionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.getId());
            response.put("totalAmount", order.getTotalPrice());
            response.put("status", order.getPaymentStatus());
            
            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.badRequest().body("Payment session not found or expired");
        }
    }
}