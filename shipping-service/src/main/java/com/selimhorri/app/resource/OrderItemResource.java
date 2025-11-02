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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.OrderItemService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/shippings")
@Slf4j
@RequiredArgsConstructor
public class OrderItemResource {

    private final OrderItemService orderItemService;

    @GetMapping
    public ResponseEntity<DtoCollectionResponse<OrderItemDto>> findAll() {
        log.info("Fetching all order items");
        return ResponseEntity.ok(
                new DtoCollectionResponse<>(this.orderItemService.findAll()));
    }

    @GetMapping(params = "orderId")
    public ResponseEntity<DtoCollectionResponse<OrderItemDto>> findAllByOrderId(
            @RequestParam("orderId") final Integer orderId) {
        log.info("Fetching order items by orderId: {}", orderId);
        return ResponseEntity.ok(
                new DtoCollectionResponse<>(this.orderItemService.findAllByOrderId(orderId)));
    }

    @GetMapping("/{orderItemId}")
    public ResponseEntity<OrderItemDto> findById(
            @PathVariable("orderItemId") final Integer orderItemId) {
        log.info("Fetching order item by id: {}", orderItemId);
        return ResponseEntity.ok(this.orderItemService.findById(orderItemId));
    }

    @GetMapping("/{orderId}/{productId}")
    public ResponseEntity<OrderItemDto> findByOrderIdAndProductId(
            @PathVariable("orderId") final Integer orderId,
            @PathVariable("productId") final Integer productId) {
        log.info("Fetching order item by composite id: orderId={}, productId={}", orderId, productId);
        return ResponseEntity.ok(this.orderItemService.findByOrderIdAndProductId(orderId, productId));
    }

    @PostMapping
    public ResponseEntity<OrderItemDto> save(
            @RequestBody 
            @NotNull(message = "Input must not be NULL") 
            @Valid final OrderItemDto orderItemDto) {
        log.info("Creating new order item");
        OrderItemDto savedItem = this.orderItemService.save(orderItemDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedItem);
    }

    @DeleteMapping("/{orderItemId}")
    public ResponseEntity<Void> deleteById(
            @PathVariable("orderItemId") final Integer orderItemId) {
        log.info("Deleting order item by id: {}", orderItemId);
        this.orderItemService.deleteById(orderItemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{orderId}/{productId}")
    public ResponseEntity<Void> deleteByOrderIdAndProductId(
            @PathVariable("orderId") final Integer orderId,
            @PathVariable("productId") final Integer productId) {
        log.info("Deleting order item by composite id: orderId={}, productId={}", orderId, productId);
        this.orderItemService.deleteByOrderIdAndProductId(orderId, productId);
        return ResponseEntity.noContent().build();
    }
}