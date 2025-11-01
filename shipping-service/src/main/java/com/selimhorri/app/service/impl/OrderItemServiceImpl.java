package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.OrderStatus;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.exception.custom.ExternalServiceException;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.exception.custom.InvalidOrderStatusException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.helper.OrderItemMappingHelper;
import com.selimhorri.app.repository.OrderItemRepository;
import com.selimhorri.app.service.OrderItemService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final RestTemplate restTemplate;

    @Override
    public List<OrderItemDto> findAll() {
        log.info("Fetching all active order items");
        
        return this.orderItemRepository.findByIsActiveTrue()
                .stream()
                .map(OrderItemMappingHelper::map)
                .filter(this::enrichOrderItemWithExternalData)
                .distinct()
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<OrderItemDto> findAllByOrderId(final int orderId) {
        log.info("Fetching order items by orderId: {}", orderId);

        return this.orderItemRepository.findAllByOrderIdAndIsActiveTrue(orderId)
                .stream()
                .map(OrderItemMappingHelper::map)
                .filter(this::enrichOrderItemWithExternalData)
                .distinct()
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public OrderItemDto findById(final int orderItemId) {
        log.info("Fetching order item by id: {}", orderItemId);

        OrderItem orderItem = this.orderItemRepository.findById(orderItemId)
                .filter(OrderItem::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ORDER_ITEM_NOT_FOUND, orderItemId));

        OrderItemDto dto = OrderItemMappingHelper.map(orderItem);

        enrichWithProduct(dto, orderItemId);
        enrichWithOrder(dto, orderItemId);

        return dto;
    }

    @Override
    public OrderItemDto findByOrderIdAndProductId(final int orderId, final int productId) {
        log.info("Fetching order item by composite id: orderId={}, productId={}", orderId, productId);

        OrderItem orderItem = this.orderItemRepository
                .findByOrderIdAndProductIdAndIsActiveTrue(orderId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ORDER_ITEM_NOT_FOUND, orderId));

        OrderItemDto dto = OrderItemMappingHelper.map(orderItem);

        enrichWithProduct(dto, orderId);
        enrichWithOrder(dto, orderId);

        return dto;
    }

    @Override
    public OrderItemDto save(final OrderItemDto orderItemDto) {
        log.info("Creating new order item");
        
        validateOrderItemInput(orderItemDto);
        verifyOrderEligibility(orderItemDto.getOrderId());
        verifyProductAvailability(orderItemDto.getProductId(), orderItemDto.getOrderedQuantity());

        OrderItemDto savedItem = OrderItemMappingHelper.map(
                this.orderItemRepository.save(
                        OrderItemMappingHelper.mapForCreation(orderItemDto)));

        updateOrderStatus(orderItemDto.getOrderId());

        log.info("Order item created successfully with id: {}", savedItem.getProductId());
        return savedItem;
    }

    @Override
    @Transactional
    public void deleteById(final int orderItemId) {
        log.info("Soft deleting order item by id: {}", orderItemId);

        this.orderItemRepository.findByOrderIdAndIsActiveTrue(orderItemId)
                .ifPresentOrElse(orderItem -> {
                    verifyOrderStatusForDeletion(orderItem.getOrderId(), orderItemId);
                    
                    orderItem.setActive(false);
                    this.orderItemRepository.save(orderItem);
                    log.info("Order item {} deactivated successfully", orderItemId);
                    
                }, () -> {
                    throw new ResourceNotFoundException(
                            ErrorCode.ORDER_ITEM_NOT_FOUND, orderItemId);
                });
    }

    @Override
    @Transactional
    public void deleteByOrderIdAndProductId(final int orderId, final int productId) {
        log.info("Soft deleting order item by composite id: orderId={}, productId={}", orderId, productId);

        this.orderItemRepository.findByOrderIdAndProductIdAndIsActiveTrue(orderId, productId)
                .ifPresentOrElse(orderItem -> {
                    verifyOrderStatusForDeletion(orderItem.getOrderId(), productId);

                    orderItem.setActive(false);
                    this.orderItemRepository.save(orderItem);
                    log.info("Order item orderId={}, productId={} deactivated successfully", orderId, productId);

                }, () -> {
                    throw new ResourceNotFoundException(
                            ErrorCode.ORDER_ITEM_NOT_FOUND, orderId);
                });
    }

    private boolean enrichOrderItemWithExternalData(OrderItemDto dto) {
        try {
            if (dto.getProductDto() != null && dto.getProductDto().getProductId() != null) {
                ProductDto product = fetchProduct(dto.getProductDto().getProductId());
                dto.setProductDto(product);
            } else {
                return false;
            }

            if (dto.getOrderDto() != null && dto.getOrderDto().getOrderId() != null) {
                OrderDto order = fetchOrder(dto.getOrderDto().getOrderId());
                
                if (!OrderStatus.ORDERED.name().equals(order.getOrderStatus())) {
                    return false;
                }
                dto.setOrderDto(order);
            } else {
                return false;
            }

            return true;
            
        } catch (ExternalServiceException e) {
            log.warn("Failed to enrich order item: {}", e.getMessage());
            return false;
        }
    }

    private void enrichWithProduct(OrderItemDto dto, int orderItemId) {
        if (dto.getProductDto() != null && dto.getProductDto().getProductId() != null) {
            try {
                ProductDto product = fetchProduct(dto.getProductDto().getProductId());
                dto.setProductDto(product);
            } catch (ExternalServiceException e) {
                log.error("Failed to fetch product for order item {}: {}", 
                        orderItemId, e.getMessage());
                throw new ExternalServiceException(
                        "Product information not available for this order item", e);
            }
        }
    }

    private void enrichWithOrder(OrderItemDto dto, int orderItemId) {
        if (dto.getOrderDto() == null || dto.getOrderDto().getOrderId() == null) {
            throw new ResourceNotFoundException(
                    ErrorCode.ORDER_ITEM_NOT_FOUND, orderItemId);
        }

        try {
            OrderDto order = fetchOrder(dto.getOrderDto().getOrderId());

            if (!OrderStatus.ORDERED.name().equals(order.getOrderStatus())) {
                throw new ResourceNotFoundException(
                        ErrorCode.ORDER_ITEM_NOT_FOUND, orderItemId);
            }

            dto.setOrderDto(order);
            
        } catch (ExternalServiceException e) {
            log.error("Failed to fetch order for order item {}: {}", 
                    orderItemId, e.getMessage());
            throw new ExternalServiceException(
                    "Order information not available for this order item", e);
        }
    }

    private void validateOrderItemInput(OrderItemDto orderItemDto) {
        if (orderItemDto.getOrderId() == null) {
            throw new InvalidInputException(
                    ErrorCode.MISSING_REQUIRED_FIELD, "Order ID is required");
        }
        if (orderItemDto.getProductId() == null) {
            throw new InvalidInputException(
                    ErrorCode.MISSING_REQUIRED_FIELD, "Product ID is required");
        }
        if (orderItemDto.getOrderedQuantity() == null) {
            throw new InvalidInputException(
                    ErrorCode.MISSING_REQUIRED_FIELD, "Ordered quantity is required");
        }
    }

    private void verifyOrderEligibility(Integer orderId) {
        try {
            OrderDto order = fetchOrder(orderId);

            if (!OrderStatus.CREATED.name().equals(order.getOrderStatus())) {
                throw new InvalidOrderStatusException(
                        ErrorCode.INVALID_ORDER_STATUS,
                        "Cannot create shipping for order with status: " + 
                        order.getOrderStatus());
            }
            
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, orderId);
        } catch (RestClientException e) {
            log.error("Error communicating with order service for order {}: {}", 
                    orderId, e.getMessage());
            throw new ExternalServiceException(
                    "Error communicating with order service", e);
        }
    }

    private void verifyProductAvailability(Integer productId, Integer orderedQuantity) {
        try {
            ProductDto product = fetchProduct(productId);

            if (product.getQuantity() < orderedQuantity) {
                throw new InvalidInputException(
                        ErrorCode.INSUFFICIENT_STOCK,
                        String.format("Insufficient stock. Available: %d, Requested: %d", 
                                product.getQuantity(), orderedQuantity));
            }
            
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, productId);
        } catch (RestClientException e) {
            log.error("Error communicating with product service for product {}: {}", 
                    productId, e.getMessage());
            throw new ExternalServiceException(
                    "Error communicating with product service", e);
        }
    }

    private void verifyOrderStatusForDeletion(Integer orderId, int orderItemId) {
        try {
            OrderDto order = fetchOrder(orderId);

            if (!OrderStatus.ORDERED.name().equals(order.getOrderStatus())) {
                throw new InvalidOrderStatusException(
                        ErrorCode.INVALID_ORDER_STATUS,
                        "Cannot delete order item - order is not in ORDERED status");
            }
            
        } catch (ExternalServiceException e) {
            log.error("Failed to verify order status for order item {}: {}", 
                    orderItemId, e.getMessage());
            throw new ExternalServiceException(
                    "Order verification failed for deletion", e);
        }
    }

    private OrderDto fetchOrder(Integer orderId) {
        try {
            String url = AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + 
                    "/" + orderId;
            log.debug("Fetching order from: {}", url);
            
            OrderDto order = this.restTemplate.getForObject(url, OrderDto.class);
            
            if (order == null) {
                throw new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, orderId);
            }
            
            return order;
            
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, orderId);
        } catch (RestClientException e) {
            log.error("Error fetching order {}: {}", orderId, e.getMessage());
            throw new ExternalServiceException(
                    "Failed to communicate with order service", e);
        }
    }

    private ProductDto fetchProduct(Integer productId) {
        try {
            String url = AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + 
                    "/" + productId;
            log.debug("Fetching product from: {}", url);
            
            ProductDto product = this.restTemplate.getForObject(url, ProductDto.class);
            
            if (product == null) {
                throw new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND, productId);
            }
            
            return product;
            
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, productId);
        } catch (RestClientException e) {
            log.error("Error fetching product {}: {}", productId, e.getMessage());
            throw new ExternalServiceException(
                    "Failed to communicate with product service", e);
        }
    }

    private void updateOrderStatus(Integer orderId) {
        try {
            String patchUrl = AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + 
                    "/" + orderId + "/status";

            this.restTemplate.patchForObject(patchUrl, null, Void.class);
            log.debug("Order status updated for order: {}", orderId);

        } catch (RestClientException e) {
            log.warn("Failed to update order status for order {}: {}", 
                    orderId, e.getMessage());
        }
    }
}