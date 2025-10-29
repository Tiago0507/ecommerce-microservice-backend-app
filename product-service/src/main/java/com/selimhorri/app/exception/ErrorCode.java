package com.selimhorri.app.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    INTERNAL_SERVER_ERROR("ERR_1000", "An unexpected error occurred"),
    SERVICE_UNAVAILABLE("ERR_1001", "Service temporarily unavailable"),
    
    VALIDATION_ERROR("ERR_2000", "Validation failed for input data"),
    INVALID_INPUT("ERR_2001", "Invalid input provided"),
    MISSING_REQUIRED_FIELD("ERR_2002", "Required field is missing"),
    INVALID_FORMAT("ERR_2003", "Invalid data format"),
    
    PRODUCT_NOT_FOUND("ERR_3000", "Product with id %s not found"),
    CATEGORY_NOT_FOUND("ERR_3001", "Category with id %s not found"),
    
    PRODUCT_ALREADY_EXISTS("ERR_4000", "Product already exists"),
    CATEGORY_ALREADY_EXISTS("ERR_4001", "Category already exists"),
    SKU_ALREADY_EXISTS("ERR_4002", "SKU '%s' already exists"),
    CATEGORY_TITLE_ALREADY_EXISTS("ERR_4003", "Category title '%s' already exists"),
    DUPLICATE_RESOURCE("ERR_4004", "Resource already exists"),

    FORBIDDEN_OPERATION("ERR_5000", "Operation not allowed"),
    RESERVED_CATEGORY_DELETE("ERR_5001", "Cannot delete reserved system categories"),
    
    DATABASE_ERROR("ERR_7000", "Database operation failed"),
    CONSTRAINT_VIOLATION("ERR_7001", "Database constraint violation"),
    DATA_INTEGRITY_ERROR("ERR_7002", "Data integrity violation");
    
    private final String code;
    private final String message;
    
    public String formatMessage(Object... args) {
        return String.format(message, args);
    }
}