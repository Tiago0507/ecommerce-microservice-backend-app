package com.selimhorri.app.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INTERNAL_SERVER_ERROR("ERR_1000", "An unexpected error occurred"),
    SERVICE_UNAVAILABLE("ERR_1001", "Service temporarily unavailable"),
    EXTERNAL_SERVICE_ERROR("ERR_1002", "External service communication failed: %s"),
    
    VALIDATION_ERROR("ERR_2000", "Validation failed for input data"),
    INVALID_INPUT("ERR_2001", "Invalid input provided"),
    MISSING_REQUIRED_FIELD("ERR_2002", "Required field is missing"),
    INVALID_FORMAT("ERR_2003", "Invalid data format"),
    
    FAVOURITE_NOT_FOUND("ERR_3005", "Favourite with id %s not found"),
    USER_NOT_FOUND("ERR_3002", "User with id %s not found"),
    PRODUCT_NOT_FOUND("ERR_3003", "Product with id %s not found"),
    
    DUPLICATE_RESOURCE("ERR_4003", "Resource already exists"),
    
    UNAUTHORIZED("ERR_5000", "Authentication required"),
    
    FORBIDDEN("ERR_6000", "You don't have permission to access this resource"),
    
    DATABASE_ERROR("ERR_7000", "Database operation failed"),
    CONSTRAINT_VIOLATION("ERR_7001", "Database constraint violation"),
    DATA_INTEGRITY_ERROR("ERR_7002", "Data integrity violation");

    private final String code;
    private final String message;

    public String formatMessage(Object... args) {
        return String.format(message, args);
    }
}