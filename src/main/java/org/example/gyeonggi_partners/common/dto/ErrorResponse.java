package org.example.gyeonggi_partners.common.dto;


import org.example.gyeonggi_partners.common.exception.ErrorCode;

public record ErrorResponse(String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getMessage());
    }
}