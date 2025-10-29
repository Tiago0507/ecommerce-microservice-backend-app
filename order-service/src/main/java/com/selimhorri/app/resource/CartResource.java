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

import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.CartService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/carts")
@Slf4j
@RequiredArgsConstructor
public class CartResource {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<DtoCollectionResponse<CartDto>> findAll() {
        log.info("GET /api/carts - Fetching all carts");
        return ResponseEntity.ok(new DtoCollectionResponse<>(this.cartService.findAll()));
    }

    @GetMapping("/{cartId}")
    public ResponseEntity<CartDto> findById(@PathVariable("cartId") final Integer cartId) {
        log.info("GET /api/carts/{} - Fetching cart by id", cartId);
        return ResponseEntity.ok(this.cartService.findById(cartId));
    }

    @PostMapping
    public ResponseEntity<CartDto> save(
            @RequestBody @NotNull(message = "Cart data must not be null") @Valid final CartDto cartDto) {
        log.info("POST /api/carts - Creating new cart");
        return ResponseEntity.status(HttpStatus.CREATED).body(this.cartService.save(cartDto));
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> deleteById(@PathVariable("cartId") final Integer cartId) {
        log.info("DELETE /api/carts/{} - Deleting cart", cartId);
        this.cartService.deleteById(cartId);
        return ResponseEntity.noContent().build();
    }
}