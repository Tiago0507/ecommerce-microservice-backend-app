package com.selimhorri.app.resource;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/orders")
@Slf4j
@RequiredArgsConstructor
public class OrderResource {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<DtoCollectionResponse<OrderDto>> findAll() {
        log.info("GET /api/orders - Fetching all orders");
        return ResponseEntity.ok(new DtoCollectionResponse<>(this.orderService.findAll()));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> findById(@PathVariable("orderId") final Integer orderId) {
        log.info("GET /api/orders/{} - Fetching order by id", orderId);
        return ResponseEntity.ok(this.orderService.findById(orderId));
    }

    @PostMapping
    public ResponseEntity<OrderDto> save(
            @RequestBody @NotNull(message = "Order data must not be null") @Valid final OrderDto orderDto) {
        log.info("POST /api/orders - Creating new order");
        return ResponseEntity.status(HttpStatus.CREATED).body(this.orderService.save(orderDto));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderDto> updateStatus(@PathVariable("orderId") final Integer orderId) {
        log.info("PATCH /api/orders/{}/status - Updating order status", orderId);
        return ResponseEntity.ok(this.orderService.updateStatus(orderId));
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<OrderDto> update(
            @PathVariable("orderId") final Integer orderId,
            @RequestBody @NotNull(message = "Order data must not be null") @Valid final OrderDto orderDto) {
        log.info("PUT /api/orders/{} - Updating order", orderId);
        return ResponseEntity.ok(this.orderService.update(orderId, orderDto));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteById(@PathVariable("orderId") final Integer orderId) {
        log.info("DELETE /api/orders/{} - Deleting order", orderId);
        this.orderService.deleteById(orderId);
        return ResponseEntity.noContent().build();
    }
}