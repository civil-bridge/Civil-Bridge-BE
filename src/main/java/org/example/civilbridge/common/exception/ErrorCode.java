package org.example.civilbridge.common.exception;

public interface ErrorCode {
    int getStatus();
    String getCode();
    String getMessage();
}