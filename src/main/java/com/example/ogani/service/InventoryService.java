package com.example.ogani.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.example.ogani.entity.InventoryAdjustment;
import com.example.ogani.entity.InventoryAdjustment.AdjustmentType;
import com.example.ogani.entity.Product;
import com.example.ogani.model.request.InventoryAdjustmentRequest;
import com.example.ogani.model.response.InventoryReportItem;

public interface InventoryService {
    
    // Product stock operations
    Product updateProductStock(long productId, int newQuantity, String reason, AdjustmentType adjustmentType);
    
    Product adjustProductStock(InventoryAdjustmentRequest request);
    
    List<Product> getLowStockProducts(int threshold);
    
    // Inventory adjustment tracking
    List<InventoryAdjustment> getAdjustmentHistory();
    
    List<InventoryAdjustment> getProductAdjustmentHistory(long productId);
    
    List<InventoryAdjustment> getAdjustmentsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    List<InventoryAdjustment> getAdjustmentsByType(AdjustmentType type);
    
    // Report generation
    List<InventoryReportItem> generateInventoryReport();
    
    List<InventoryReportItem> generateLowStockReport(int threshold);
    
    Map<String, String> getAdjustmentTypes();
} 