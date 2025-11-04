package com.selimhorri.e2e.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Integer orderId;
    
    @JsonFormat(pattern = "dd-MM-yyyy__HH:mm:ss:SSSSSS")
    private LocalDateTime orderDate;
    
    private String orderDesc;
    private Double orderFee;
    private String orderStatus;
    private CartDto cartDto;
}
