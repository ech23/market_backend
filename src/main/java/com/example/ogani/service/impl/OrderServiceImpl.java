package com.example.ogani.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ogani.entity.InventoryAdjustment;
import com.example.ogani.entity.InventoryAdjustment.AdjustmentType;
import com.example.ogani.entity.Order;
import com.example.ogani.entity.OrderDetail;
import com.example.ogani.entity.OrderStatus;
import com.example.ogani.entity.Product;
import com.example.ogani.entity.User;
import com.example.ogani.exception.InsufficientStockException;
import com.example.ogani.exception.NotFoundException;
import com.example.ogani.model.request.CreateOrderDetailRequest;
import com.example.ogani.model.request.CreateOrderRequest;
import com.example.ogani.repository.InventoryAdjustmentRepository;
import com.example.ogani.repository.OrderDetailRepository;
import com.example.ogani.repository.OrderRepository;
import com.example.ogani.repository.ProductRepository;
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
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private InventoryAdjustmentRepository inventoryAdjustmentRepository;

    @Override
    @Transactional
    public Order placeOrder(CreateOrderRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new NotFoundException("Not Found User With Username:" + request.getUsername()));

        // Verify product stock before creating order
        Map<Long, Integer> productQuantities = new HashMap<>();
        List<String> insufficientStockMessages = new ArrayList<>();
        
        // Check each product's available quantity
        for (CreateOrderDetailRequest rq : request.getOrderDetails()) {
            Long productId = rq.getProductId();
            int requestedQuantity = rq.getQuantity();
            
            // Get current stock
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new NotFoundException("Product not found with ID: " + productId));
            
            int availableQuantity = product.getQuantity();
            
            // If requested quantity is more than what's available
            if (requestedQuantity > availableQuantity) {
                insufficientStockMessages.add("Sản phẩm '" + product.getName() + 
                        "' chỉ còn " + availableQuantity + " sản phẩm, không đủ số lượng " + requestedQuantity);
            }
            
            // Keep track of product quantities for later update
            productQuantities.put(productId, requestedQuantity);
        }
        
        // If any product has insufficient stock, throw exception
        if (!insufficientStockMessages.isEmpty()) {
            throw new InsufficientStockException(String.join("; ", insufficientStockMessages));
        }
        
        // Create order
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
        order.setOrderStatus(OrderStatus.PENDING);

        long totalPrice = 0;
        order = orderRepository.save(order);  // Save lần 1 để có ID cho OrderDetail

        for (CreateOrderDetailRequest rq : request.getOrderDetails()) {
            // Get the product
            Product product = productRepository.findById(rq.getProductId()).get();
            
            // Create order detail with product reference
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setName(rq.getName());
            orderDetail.setPrice(rq.getPrice());
            orderDetail.setQuantity(rq.getQuantity());
            orderDetail.setSubTotal(rq.getPrice() * rq.getQuantity());
            orderDetail.setOrder(order);
            orderDetail.setProduct(product); // Set the product reference
            totalPrice += orderDetail.getSubTotal();
            orderDetailRepository.save(orderDetail);
            
            // Reduce product quantity
            int previousStock = product.getQuantity();
            int newStock = previousStock - rq.getQuantity();
            product.setQuantity(newStock);
            productRepository.save(product);
            
            // Track inventory adjustment
            InventoryAdjustment adjustment = new InventoryAdjustment();
            adjustment.setProduct(product);
            adjustment.setQuantity(-rq.getQuantity()); // Negative quantity for reduction
            adjustment.setPreviousStock(previousStock);
            adjustment.setNewStock(newStock);
            adjustment.setAdjustmentDate(LocalDateTime.now());
            adjustment.setAdjustmentReason("Order #" + order.getId());
            adjustment.setAdjustedBy(request.getUsername());
            adjustment.setAdjustmentType(AdjustmentType.ORDER_PLACEMENT);
            inventoryAdjustmentRepository.save(adjustment);
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
    public Order getOrderById(long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found with ID: " + id));
    }

    @Override
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByOrderStatus(status);
    }
    
    @Override
    public List<Order> getOrdersByStatusAndUser(OrderStatus status, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Not Found User With Username:" + username));
        return orderRepository.findByOrderStatusAndUser_Id(status, user.getId());
    }

    @Override
    public Order updateOrderPaymentStatus(long id, String status, String paymentMethod) {
        Order order = getOrderById(id);
        order.setPaymentStatus(status);
        order.setPaymentMethod(paymentMethod);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order updateOrderStatus(long id, OrderStatus status) {
        Order order = getOrderById(id);
        OrderStatus previousStatus = order.getOrderStatus();
        order.setOrderStatus(status);
        
        // Handle inventory adjustments when cancelling an order
        if (status == OrderStatus.CANCELLED && previousStatus != OrderStatus.CANCELLED) {
            // Return items to inventory
            for (OrderDetail detail : order.getOrderdetails()) {
                Product product = detail.getProduct();
                if (product != null) {
                    int previousStock = product.getQuantity();
                    int returnedQuantity = detail.getQuantity();
                    int newStock = previousStock + returnedQuantity;
                    
                    // Update product stock
                    product.setQuantity(newStock);
                    productRepository.save(product);
                    
                    // Track inventory adjustment
                    InventoryAdjustment adjustment = new InventoryAdjustment();
                    adjustment.setProduct(product);
                    adjustment.setQuantity(returnedQuantity); // Positive for return to inventory
                    adjustment.setPreviousStock(previousStock);
                    adjustment.setNewStock(newStock);
                    adjustment.setAdjustmentDate(LocalDateTime.now());
                    adjustment.setAdjustmentReason("Cancelled Order #" + order.getId());
                    adjustment.setAdjustedBy("system");
                    adjustment.setAdjustmentType(AdjustmentType.ORDER_CANCELLATION);
                    inventoryAdjustmentRepository.save(adjustment);
                }
            }
        }
        
        return orderRepository.save(order);
    }
}
