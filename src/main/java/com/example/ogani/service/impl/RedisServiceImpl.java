package com.example.ogani.service.impl;

import com.example.ogani.entity.Order;
import com.example.ogani.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class RedisServiceImpl implements RedisService {

    private static final String ORDER_KEY_PREFIX = "order:payment:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Value("${redis.ttl.order}")
    private long orderTtl;
    
    @Override
    public String storeOrderForPayment(Order order, String paymentSessionId) {
        String key = ORDER_KEY_PREFIX + paymentSessionId;
        redisTemplate.opsForValue().set(key, order, orderTtl, TimeUnit.SECONDS);
        return paymentSessionId;
    }
    
    @Override
    public Optional<Order> getOrderByPaymentSessionId(String paymentSessionId) {
        String key = ORDER_KEY_PREFIX + paymentSessionId;
        Object order = redisTemplate.opsForValue().get(key);
        if (order == null) {
            return Optional.empty();
        }
        return Optional.of((Order) order);
    }
    
    @Override
    public void removeOrder(String paymentSessionId) {
        String key = ORDER_KEY_PREFIX + paymentSessionId;
        redisTemplate.delete(key);
    }
}