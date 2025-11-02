package com.selimhorri.app.exception.custom;

import com.selimhorri.app.exception.ErrorCode;

public class InvalidOrderStatusException extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    public InvalidOrderStatusException(String message) {
        super(message);
        this.errorCode = ErrorCode.INVALID_ORDER_STATUS;
    }
    
    public InvalidOrderStatusException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public InvalidOrderStatusException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}