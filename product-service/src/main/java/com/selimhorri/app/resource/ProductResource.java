package com.selimhorri.app.resource;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/products")
@Slf4j
@RequiredArgsConstructor
public class ProductResource {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<DtoCollectionResponse<ProductDto>> findAll() {
        log.info("Fetching all products");
        return ResponseEntity.ok(new DtoCollectionResponse<>(this.productService.findAll()));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDto> findById(@PathVariable("productId") final Integer productId) {
        log.info("Fetching product with id: {}", productId);
        return ResponseEntity.ok(this.productService.findById(productId));
    }

    @PostMapping
    public ResponseEntity<ProductDto> save(
            @RequestBody @NotNull(message = "Request body cannot be null") @Valid final ProductDto productDto) {
        log.info("Saving new product");
        return ResponseEntity.ok(this.productService.save(productDto));
    }

    @PutMapping
    public ResponseEntity<ProductDto> update(
            @RequestBody @NotNull(message = "Request body cannot be null") @Valid final ProductDto productDto) {
        log.info("Updating product");
        return ResponseEntity.ok(this.productService.update(productDto));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductDto> update(
            @PathVariable("productId") final Integer productId,
            @RequestBody @NotNull(message = "Request body cannot be null") @Valid final ProductDto productDto) {
        log.info("Updating product with id: {}", productId);
        return ResponseEntity.ok(this.productService.update(productId, productDto));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Boolean> deleteById(@PathVariable("productId") final Integer productId) {
        log.info("Deleting product with id: {}", productId);
        this.productService.deleteById(productId);
        return ResponseEntity.ok(true);
    }
}