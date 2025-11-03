package com.selimhorri.app.util;

import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.exception.ErrorCode;

public final class ParserUtil {

    private ParserUtil() {}

    public static Integer parseId(final String value, final String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT, fieldName + " must not be blank");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT, fieldName + " must be a valid integer");
        }
    }
}