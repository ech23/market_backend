package com.example.ogani.model.request;

import com.example.ogani.entity.InventoryAdjustment.AdjustmentType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryAdjustmentRequest {
    private long productId;
    private int quantity;
    private String reason;
    private AdjustmentType adjustmentType;
    private String username; // user performing the adjustment
} 