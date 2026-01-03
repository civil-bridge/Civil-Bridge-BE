package org.example.gyeonggi_partners.common.exception;


import lombok.extern.slf4j.Slf4j;
import org.example.gyeonggi_partners.common.dto.ApiResponse;
import org.example.gyeonggi_partners.common.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 우리가 직접 정의한 BusinessException을 처리합니다.
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<?>> handleBusinessException(final BusinessException e) {
        final ErrorCode errorCode = e.getErrorCode();
        log.error("handleBusinessException : {}", errorCode.getMessage());

        final ApiResponse<?> response = ApiResponse.error(errorCode);

        return new ResponseEntity<>(response, HttpStatus.valueOf(errorCode.getStatus()));
    }

}