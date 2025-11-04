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
public class FavouriteDto {
    private Integer userId;
    private Integer productId;
    
    @JsonFormat(pattern = "dd-MM-yyyy__HH:mm:ss:SSSSSS")
    private LocalDateTime likeDate;
    
    private UserDto userDto;
    private ProductDto productDto;
}
