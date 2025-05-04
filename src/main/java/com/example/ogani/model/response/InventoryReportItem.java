package com.example.ogani.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryReportItem {
    private long productId;
    private String productName;
    private String categoryName;
    private int currentStock;
    private boolean lowStock;
    private int minimumStockLevel;
    private int totalAdjustments;
    private long price;
    private long stockValue; // price * currentStock
} 