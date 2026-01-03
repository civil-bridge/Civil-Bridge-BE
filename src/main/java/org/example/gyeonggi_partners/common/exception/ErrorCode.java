package org.example.gyeonggi_partners.common.exception;

public interface ErrorCode {
    int getStatus();
    String getCode();
    String getMessage();
}