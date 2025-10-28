package com.selimhorri.app.util;

import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.exception.custom.InvalidInputException;

public class ParserUtil {
    
    public static Integer parseId(String id, String fieldName) {
        if (id == null || id.trim().isEmpty()) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT, 
                fieldName + " cannot be empty");
        }
        
        try {
            return Integer.parseInt(id.trim());
        } catch (NumberFormatException e) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT, 
                "Invalid " + fieldName + " format: " + id);
        }
    }
}