package com.example.ogani.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.ogani.entity.Order;
import com.example.ogani.entity.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long> {
    
    @Query(value ="Select * from Orders where user_id = :id order by id desc",nativeQuery = true)
    List<Order> getOrderByUser(long id);
    
    Optional<Order> findById(long id);
    
    List<Order> findByOrderStatus(OrderStatus status);
    
    List<Order> findByOrderStatusAndUser_Id(OrderStatus status, long userId);
}
