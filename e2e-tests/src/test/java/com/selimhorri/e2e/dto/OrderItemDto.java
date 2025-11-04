package com.selimhorri.e2e.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {
    private Integer productId;
    private Integer orderId;
    private Integer orderedQuantity;
    private ProductDto productDto;
    private OrderDto orderDto;
}
