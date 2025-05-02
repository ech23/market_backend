package com.example.ogani.model.request;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderDetailRequest {

    @NotNull(message="Tên sản phẩm rỗng")
    @NotEmpty(message = "Tên sản phẩm rỗng")
    @Size(min=5,max=50,message="Tên sản phẩm từ 5-50 ký tự")
    private String name;

    @NotNull(message="Giá sản phẩm rỗng")
    private long price;

    @NotNull(message = "Số lượng sản phẩm rỗng")
    private int quantity;

    private long subTotal;
    
    @NotNull(message = "ID sản phẩm không được để trống")
    private Long productId;
}
