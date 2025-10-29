package com.selimhorri.app.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    INTERNAL_SERVER_ERROR("ERR_1000", "An unexpected error occurred"),
    SERVICE_UNAVAILABLE("ERR_1001", "Service temporarily unavailable"),
    EXTERNAL_SERVICE_ERROR("ERR_1002", "External service communication failed"),
    
    VALIDATION_ERROR("ERR_2000", "Validation failed for input data"),
    INVALID_INPUT("ERR_2001", "Invalid input provided"),
    MISSING_REQUIRED_FIELD("ERR_2002", "Required field is missing"),
    INVALID_FORMAT("ERR_2003", "Invalid data format"),
    
    ORDER_NOT_FOUND("ERR_3000", "Order with id %s not found"),
    CART_NOT_FOUND("ERR_3001", "Cart with id %s not found"),
    USER_NOT_FOUND("ERR_3002", "User with id %s not found"),
    PRODUCT_NOT_FOUND("ERR_3003", "Product with id %s not found"),
    
    CART_ALREADY_EXISTS("ERR_4000", "Cart already exists for this user"),
    ORDER_ALREADY_EXISTS("ERR_4001", "Order already exists"),
    DUPLICATE_RESOURCE("ERR_4002", "Resource already exists"),
    
    INVALID_ORDER_STATUS("ERR_5000", "Invalid order status transition"),
    ORDER_ALREADY_COMPLETED("ERR_5001", "Order is already completed and cannot be modified"),
    CART_INACTIVE("ERR_5002", "Cannot perform operation on inactive cart"),
    EMPTY_CART("ERR_5003", "Cannot create order from empty cart"),
    
    DATABASE_ERROR("ERR_7000", "Database operation failed"),
    CONSTRAINT_VIOLATION("ERR_7001", "Database constraint violation"),
    DATA_INTEGRITY_ERROR("ERR_7002", "Data integrity violation");
    
    private final String code;
    private final String message;
    
    public String formatMessage(Object... args) {
        return String.format(message, args);
    }
}