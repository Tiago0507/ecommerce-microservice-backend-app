package com.selimhorri.app.exception.custom;

import com.selimhorri.app.exception.ErrorCode;

public class ResourceNotFoundException extends RuntimeException {

    private final ErrorCode errorCode;

    public ResourceNotFoundException(ErrorCode errorCode, Object... args) {
        super(errorCode.formatMessage(args));
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}