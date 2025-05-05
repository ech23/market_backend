package com.example.ogani.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.ogani.entity.InventoryAdjustment;

@Repository
public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {
    
    List<InventoryAdjustment> findByProduct_Id(long productId);
    
    List<InventoryAdjustment> findByProduct_IdOrderByAdjustmentDateDesc(long productId);
    
    List<InventoryAdjustment> findByAdjustmentDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT ia FROM InventoryAdjustment ia WHERE ia.product.id = :productId AND ia.adjustmentDate BETWEEN :startDate AND :endDate ORDER BY ia.adjustmentDate DESC")
    List<InventoryAdjustment> findByProductAndDateRange(long productId, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT ia FROM InventoryAdjustment ia WHERE ia.adjustmentType = :adjustmentType ORDER BY ia.adjustmentDate DESC")
    List<InventoryAdjustment> findByAdjustmentType(InventoryAdjustment.AdjustmentType adjustmentType);
} 