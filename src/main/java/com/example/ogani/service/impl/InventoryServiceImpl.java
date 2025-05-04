package com.example.ogani.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ogani.entity.InventoryAdjustment;
import com.example.ogani.entity.InventoryAdjustment.AdjustmentType;
import com.example.ogani.entity.Product;
import com.example.ogani.exception.InsufficientStockException;
import com.example.ogani.exception.NotFoundException;
import com.example.ogani.model.request.InventoryAdjustmentRequest;
import com.example.ogani.model.response.InventoryReportItem;
import com.example.ogani.repository.InventoryAdjustmentRepository;
import com.example.ogani.repository.ProductRepository;
import com.example.ogani.service.InventoryService;

@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private InventoryAdjustmentRepository inventoryAdjustmentRepository;
    
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    @Override
    @Transactional
    @CacheEvict(value = {"productList", "product"}, allEntries = true)
    public Product updateProductStock(long productId, int newQuantity, String reason, AdjustmentType adjustmentType) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with ID: " + productId));
        
        int previousStock = product.getQuantity();
        product.setQuantity(newQuantity);
        
        // Create stock adjustment record
        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setProduct(product);
        adjustment.setQuantity(newQuantity - previousStock);
        adjustment.setPreviousStock(previousStock);
        adjustment.setNewStock(newQuantity);
        adjustment.setAdjustmentDate(LocalDateTime.now());
        adjustment.setAdjustmentReason(reason);
        adjustment.setAdjustmentType(adjustmentType);

        
        // Determine adjustment type
        if (newQuantity > previousStock) {
            adjustment.setAdjustmentType(AdjustmentType.ADD);
        } else if (newQuantity < previousStock) {
            adjustment.setAdjustmentType(AdjustmentType.SUBTRACT);
        }
        
        inventoryAdjustmentRepository.save(adjustment);
        return productRepository.save(product);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = {"productList", "product"}, allEntries = true)
    public Product adjustProductStock(InventoryAdjustmentRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found with ID: " + request.getProductId()));
        
        int previousStock = product.getQuantity();
        int newStock;
        
        // Handle different adjustment types
        switch (request.getAdjustmentType()) {
            case ADD:
                newStock = previousStock + request.getQuantity();
                break;
            case SUBTRACT:
                newStock = previousStock - request.getQuantity();
                if (newStock < 0) {
                    throw new InsufficientStockException("Cannot reduce stock below zero. Current stock: " 
                            + previousStock + ", Requested reduction: " + request.getQuantity());
                }
                break;

            default:
                newStock = previousStock;
        }
        
        product.setQuantity(newStock);
        
        // Create adjustment record
        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setProduct(product);
        adjustment.setQuantity(newStock - previousStock);
        adjustment.setPreviousStock(previousStock);
        adjustment.setNewStock(newStock);
        adjustment.setAdjustmentDate(LocalDateTime.now());
        adjustment.setAdjustmentReason(request.getReason());
        adjustment.setAdjustedBy(request.getUsername());
        adjustment.setAdjustmentType(request.getAdjustmentType());
        
        inventoryAdjustmentRepository.save(adjustment);
        return productRepository.save(product);
    }
    
    @Override
    @Cacheable(value = "lowStockProducts", key = "#threshold")
    public List<Product> getLowStockProducts(int threshold) {
        List<Product> allProducts = productRepository.findAll();
        
        return allProducts.stream()
                .filter(product -> product.getQuantity() < threshold)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<InventoryAdjustment> getAdjustmentHistory() {
        return inventoryAdjustmentRepository.findAll();
    }
    
    @Override
    public List<InventoryAdjustment> getProductAdjustmentHistory(long productId) {
        // Verify product exists
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException("Product not found with ID: " + productId);
        }
        
        return inventoryAdjustmentRepository.findByProduct_IdOrderByAdjustmentDateDesc(productId);
    }
    
    @Override
    public List<InventoryAdjustment> getAdjustmentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return inventoryAdjustmentRepository.findByAdjustmentDateBetween(startDate, endDate);
    }
    
    @Override
    public List<InventoryAdjustment> getAdjustmentsByType(AdjustmentType type) {
        return inventoryAdjustmentRepository.findByAdjustmentType(type);
    }
    
    @Override
    public List<InventoryReportItem> generateInventoryReport() {
        List<Product> products = productRepository.findAll();
        List<InventoryReportItem> reportItems = new ArrayList<>();
        
        for (Product product : products) {
            InventoryReportItem item = createReportItem(product);
            reportItems.add(item);
        }
        
        return reportItems;
    }
    
    @Override
    public List<InventoryReportItem> generateLowStockReport(int threshold) {
        List<Product> lowStockProducts = getLowStockProducts(threshold);
        List<InventoryReportItem> reportItems = new ArrayList<>();
        
        for (Product product : lowStockProducts) {
            InventoryReportItem item = createReportItem(product);
            reportItems.add(item);
        }
        
        return reportItems;
    }
    
    @Override
    public Map<String, String> getAdjustmentTypes() {
        return Arrays.stream(AdjustmentType.values())
                .collect(Collectors.toMap(
                        type -> type.name(),
                        type -> type.getDisplayName()
                ));
    }
    
    private InventoryReportItem createReportItem(Product product) {
        InventoryReportItem item = new InventoryReportItem();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setCategoryName(product.getCategory().getName());
        item.setCurrentStock(product.getQuantity());
        item.setLowStock(product.getQuantity() < DEFAULT_LOW_STOCK_THRESHOLD);
        item.setMinimumStockLevel(DEFAULT_LOW_STOCK_THRESHOLD);
        item.setPrice(product.getPrice());
        item.setStockValue(product.getPrice() * product.getQuantity());
        
        // Count total adjustments for this product
        List<InventoryAdjustment> adjustments = 
                inventoryAdjustmentRepository.findByProduct_Id(product.getId());
        item.setTotalAdjustments(adjustments.size());
        
        return item;
    }
} 