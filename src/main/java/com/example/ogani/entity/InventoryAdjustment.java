package com.example.ogani.entity;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "inventory_adjustment")
public class InventoryAdjustment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    
    private int quantity;  // Can be positive (addition) or negative (reduction)
    
    private int previousStock;
    
    private int newStock;
    
    private LocalDateTime adjustmentDate;
    
    private String adjustmentReason;
    
    private String adjustedBy;  // Username who made the adjustment
    
    @Enumerated(EnumType.STRING)
    private AdjustmentType adjustmentType;
    
    public enum AdjustmentType {
        ADD("Thêm hàng"),
        SUBTRACT("Giảm hàng"),
        INVENTORY_CORRECTION("Điều chỉnh hàng tồn"),
        ORDER_PLACEMENT("Đặt hàng"),
        ORDER_CANCELLATION("Hủy đơn hàng"),
        RETURNED_ITEM("Hàng trả về");
        
        private final String displayName;
        
        AdjustmentType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
} 