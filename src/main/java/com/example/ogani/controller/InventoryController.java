package com.example.ogani.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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

import com.example.ogani.entity.InventoryAdjustment;
import com.example.ogani.entity.InventoryAdjustment.AdjustmentType;
import com.example.ogani.entity.Product;
import com.example.ogani.exception.InsufficientStockException;
import com.example.ogani.exception.NotFoundException;
import com.example.ogani.model.request.InventoryAdjustmentRequest;
import com.example.ogani.model.response.InventoryReportItem;
import com.example.ogani.model.response.MessageResponse;
import com.example.ogani.service.InventoryService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/inventory")

public class InventoryController {

    @Autowired
    private InventoryService inventoryService;
    
    @PostMapping("/adjust")
    @Operation(summary = "Điều chỉnh số lượng hàng tồn kho")
    public ResponseEntity<?> adjustInventory(@RequestBody InventoryAdjustmentRequest request) {
        try {
            Product product = inventoryService.adjustProductStock(request);
            return ResponseEntity.ok(product);
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
    
    @PostMapping("/update/{id}")
    @Operation(summary = "Cập nhật số lượng hàng tồn kho")
    public ResponseEntity<?> updateInventory(
            @PathVariable("id") long productId,
            @RequestParam("quantity") int quantity,
            @RequestParam("reason") String reason,
            @RequestParam("adjustmentType") AdjustmentType adjustmentType)
             {
        try {
            Product product = inventoryService.updateProductStock(productId, quantity, reason, adjustmentType);
            return ResponseEntity.ok(product);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Có lỗi xảy ra: " + e.getMessage()));
        }
    }
    
    @GetMapping("/low-stock")
    @Operation(summary = "Lấy danh sách sản phẩm có số lượng tồn thấp")
    public ResponseEntity<List<Product>> getLowStockProducts(
            @RequestParam(value = "threshold", defaultValue = "10") int threshold) {
        List<Product> products = inventoryService.getLowStockProducts(threshold);
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/adjustments")
    @Operation(summary = "Lấy lịch sử điều chỉnh hàng tồn kho")
    public ResponseEntity<List<InventoryAdjustment>> getAdjustmentHistory() {
        List<InventoryAdjustment> history = inventoryService.getAdjustmentHistory();
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/adjustments/{id}")
    @Operation(summary = "Lấy lịch sử điều chỉnh hàng tồn kho của một sản phẩm")
    public ResponseEntity<?> getProductAdjustmentHistory(@PathVariable("id") long productId) {
        try {
            List<InventoryAdjustment> history = inventoryService.getProductAdjustmentHistory(productId);
            return ResponseEntity.ok(history);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/adjustments/date-range")
    @Operation(summary = "Lấy lịch sử điều chỉnh hàng tồn kho theo khoảng thời gian")
    public ResponseEntity<List<InventoryAdjustment>> getAdjustmentsByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<InventoryAdjustment> history = inventoryService.getAdjustmentsByDateRange(startDate, endDate);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/adjustments/type/{type}")
    @Operation(summary = "Lấy lịch sử điều chỉnh hàng tồn kho theo loại điều chỉnh")
    public ResponseEntity<?> getAdjustmentsByType(@PathVariable("type") String typeStr) {
        try {
            AdjustmentType type = AdjustmentType.valueOf(typeStr);
            List<InventoryAdjustment> history = inventoryService.getAdjustmentsByType(type);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Loại điều chỉnh không hợp lệ: " + typeStr));
        }
    }
    
    @GetMapping("/report")
    @Operation(summary = "Tạo báo cáo tồn kho")
    public ResponseEntity<List<InventoryReportItem>> generateInventoryReport() {
        List<InventoryReportItem> report = inventoryService.generateInventoryReport();
        return ResponseEntity.ok(report);
    }
    
    @GetMapping("/report/low-stock")
    @Operation(summary = "Tạo báo cáo sản phẩm có số lượng tồn thấp")
    public ResponseEntity<List<InventoryReportItem>> generateLowStockReport(
            @RequestParam(value = "threshold", defaultValue = "10") int threshold) {
        List<InventoryReportItem> report = inventoryService.generateLowStockReport(threshold);
        return ResponseEntity.ok(report);
    }
    
    @GetMapping("/adjustment-types")
    @Operation(summary = "Lấy danh sách các loại điều chỉnh hàng tồn kho")
    public ResponseEntity<Map<String, String>> getAdjustmentTypes() {
        Map<String, String> types = inventoryService.getAdjustmentTypes();
        return ResponseEntity.ok(types);
    }
} 