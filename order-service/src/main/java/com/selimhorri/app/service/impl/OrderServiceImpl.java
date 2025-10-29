package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.selimhorri.app.domain.Order;
import com.selimhorri.app.domain.enums.OrderStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.exception.custom.InvalidOrderStatusException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.helper.OrderMappingHelper;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;

    @Override
    public List<OrderDto> findAll() {
        log.info("Fetching all active orders");
        return this.orderRepository.findAllByIsActiveTrue()
                .stream()
                .map(OrderMappingHelper::map)
                .distinct()
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public OrderDto findById(final Integer orderId) {
        log.info("Fetching active order with id: {}", orderId);
        return this.orderRepository.findByOrderIdAndIsActiveTrue(orderId)
                .map(OrderMappingHelper::map)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, orderId));
    }

    @Override
    public OrderDto save(final OrderDto orderDto) {
        log.info("Creating new order");
        
        orderDto.setOrderId(null);
        orderDto.setOrderStatus(null);

        if (orderDto.getCartDto() == null || orderDto.getCartDto().getCartId() == null) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD, 
                    "Order must be associated with a cart");
        }

        cartRepository.findById(orderDto.getCartDto().getCartId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CART_NOT_FOUND, 
                        orderDto.getCartDto().getCartId()));

        return OrderMappingHelper.map(
                this.orderRepository.save(OrderMappingHelper.mapForCreationOrder(orderDto)));
    }

    @Override
    public OrderDto updateStatus(final int orderId) {
        log.info("Updating status for order with id: {}", orderId);

        Order existingOrder = this.orderRepository.findByOrderIdAndIsActiveTrue(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, orderId));

        OrderStatus newStatus;
        switch (existingOrder.getStatus()) {
            case CREATED:
                newStatus = OrderStatus.ORDERED;
                break;
            case ORDERED:
                newStatus = OrderStatus.IN_PAYMENT;
                break;
            case IN_PAYMENT:
                throw new InvalidOrderStatusException(ErrorCode.ORDER_ALREADY_COMPLETED,
                        "Order is already paid and cannot be updated further");
            default:
                throw new InvalidOrderStatusException(ErrorCode.INVALID_ORDER_STATUS,
                        "Unknown order status: " + existingOrder.getStatus());
        }

        existingOrder.setStatus(newStatus);
        Order updatedOrder = this.orderRepository.save(existingOrder);

        log.info("Order status updated successfully from {} to {}", existingOrder.getStatus(), newStatus);

        return OrderMappingHelper.map(updatedOrder);
    }

    @Override
    public OrderDto update(final Integer orderId, final OrderDto orderDto) {
        log.info("Updating order with id: {}", orderId);
        
        orderDto.setOrderStatus(null);

        Order existingOrder = this.orderRepository.findByOrderIdAndIsActiveTrue(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, orderId));

        orderDto.setOrderId(orderId);
        orderDto.setOrderStatus(existingOrder.getStatus());
        
        Order updatedOrder = OrderMappingHelper.mapForUpdate(orderDto, existingOrder.getCart());
        updatedOrder.setOrderDate(existingOrder.getOrderDate());
        
        return OrderMappingHelper.map(this.orderRepository.save(updatedOrder));
    }

    @Override
    public void deleteById(final Integer orderId) {
        log.info("Soft deleting order with id: {}", orderId);

        Order order = orderRepository.findByOrderIdAndIsActiveTrue(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, orderId));

        if (order.getStatus() == OrderStatus.IN_PAYMENT) {
            throw new InvalidOrderStatusException(ErrorCode.ORDER_ALREADY_COMPLETED,
                    "Cannot delete order that is already paid");
        }

        order.setActive(false);
        orderRepository.save(order);
        
        log.debug("Order with id: {} successfully deactivated", orderId);
    }
}