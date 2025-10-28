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
    
    USER_NOT_FOUND("ERR_3000", "User with id %s not found"),
    CREDENTIAL_NOT_FOUND("ERR_3001", "Credential with id %s not found"),
    ADDRESS_NOT_FOUND("ERR_3002", "Address with id %s not found"),
    VERIFICATION_TOKEN_NOT_FOUND("ERR_3003", "Verification token with id %s not found"),
    USERNAME_NOT_FOUND("ERR_3004", "User with username %s not found"),
    
    USER_ALREADY_EXISTS("ERR_4000", "User already exists"),
    USERNAME_ALREADY_TAKEN("ERR_4001", "Username '%s' is already taken"),
    EMAIL_ALREADY_REGISTERED("ERR_4002", "Email '%s' is already registered"),
    DUPLICATE_RESOURCE("ERR_4003", "Resource already exists"),
    
    UNAUTHORIZED("ERR_5000", "Authentication required"),
    INVALID_CREDENTIALS("ERR_5001", "Invalid credentials provided"),
    TOKEN_EXPIRED("ERR_5002", "Authentication token has expired"),
    TOKEN_INVALID("ERR_5003", "Authentication token is invalid"),
    
    FORBIDDEN("ERR_6000", "You don't have permission to access this resource"),
    INSUFFICIENT_PERMISSIONS("ERR_6001", "Insufficient permissions"),
    
    DATABASE_ERROR("ERR_7000", "Database operation failed"),
    CONSTRAINT_VIOLATION("ERR_7001", "Database constraint violation"),
    DATA_INTEGRITY_ERROR("ERR_7002", "Data integrity violation");
    
    private final String code;
    private final String message;
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String formatMessage(Object... args) {
        return String.format(message, args);
    }
}