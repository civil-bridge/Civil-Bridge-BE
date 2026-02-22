package org.example.civilbridge.common.dto;


import org.example.civilbridge.common.exception.ErrorCode;

public record ErrorResponse(String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getMessage());
    }
}