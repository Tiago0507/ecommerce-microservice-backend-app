package com.selimhorri.app.resource;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.service.FavouriteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/favourites")
@Slf4j
@RequiredArgsConstructor
public class FavouriteResource {
    
    private final FavouriteService favouriteService;
    
    @GetMapping
    public ResponseEntity<DtoCollectionResponse<FavouriteDto>> findAll() {
        log.info("Fetching all favourites");
        return ResponseEntity.ok(new DtoCollectionResponse<>(this.favouriteService.findAll()));
    }
    
    @GetMapping("/{userId}/{productId}")
    public ResponseEntity<FavouriteDto> findById(
            @PathVariable("userId") final String userId, 
            @PathVariable("productId") final String productId) {
        
        log.info("Fetching favourite by userId: {} and productId: {}", userId, productId);
        
        Integer userIdInt = parseId(userId, "userId");
        Integer productIdInt = parseId(productId, "productId");
        
        FavouriteDto favourite = this.favouriteService.findById(
                new FavouriteId(userIdInt, productIdInt, null));
        
        return ResponseEntity.ok(favourite);
    }
    
    @PostMapping
    public ResponseEntity<FavouriteDto> save(
            @RequestBody 
            @NotNull(message = "Input must not be NULL") 
            @Valid final FavouriteDto favouriteDto) {
        
        log.info("Creating favourite for user: {} and product: {}", 
                favouriteDto.getUserId(), favouriteDto.getProductId());
        
        FavouriteDto savedFavourite = this.favouriteService.save(favouriteDto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFavourite);
    }
    
    @DeleteMapping("/{userId}/{productId}")
    public ResponseEntity<Void> deleteById(
            @PathVariable("userId") final String userId, 
            @PathVariable("productId") final String productId) {
        
        log.info("Deleting favourite by userId: {} and productId: {}", userId, productId);
        
        Integer userIdInt = parseId(userId, "userId");
        Integer productIdInt = parseId(productId, "productId");
        
        this.favouriteService.deleteById(new FavouriteId(userIdInt, productIdInt, null));
        
        return ResponseEntity.noContent().build();
    }
    
    private Integer parseId(String id, String fieldName) {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            log.warn("Invalid {} format: {}", fieldName, id);
            throw new InvalidInputException(
                    ErrorCode.INVALID_FORMAT, 
                    String.format("Invalid %s format. Must be a valid integer", fieldName));
        }
    }
}