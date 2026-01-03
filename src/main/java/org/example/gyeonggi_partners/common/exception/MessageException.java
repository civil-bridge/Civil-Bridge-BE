package org.example.gyeonggi_partners.common.exception;

import lombok.Getter;

public class MessageException extends RuntimeException {

    @Getter
    private final ErrorCode errorCode;
    public MessageException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
