package com.selimhorri.app.exception.custom;

import com.selimhorri.app.exception.ErrorCode;

import lombok.Getter;

@Getter
public class ForbiddenOperationException extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    public ForbiddenOperationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public ForbiddenOperationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}