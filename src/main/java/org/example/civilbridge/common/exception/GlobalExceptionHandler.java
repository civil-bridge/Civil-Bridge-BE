package org.example.civilbridge.common.exception;


import lombok.extern.slf4j.Slf4j;
import org.example.civilbridge.common.dto.ApiResponse;
import org.example.civilbridge.common.dto.ErrorResponse;
import org.example.civilbridge.domain.proposal.exception.ProposalErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

    /**
     * 도메인 모델의 유효성 검증 실패(IllegalArgumentException)를 처리합니다.
     * (예: 제목 길이 초과, 본문 내용 누락 등)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(final IllegalArgumentException e) {
        log.error("handleIllegalArgumentException : {}", e.getMessage());

        final ApiResponse<?> response = new ApiResponse<>("INVALID_REQUEST", e.getMessage(), null);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 낙관적 락 충돌에 대한 안전망 핸들러.
     * (updateContent JPQL 직접 업데이트로 일반 편집 경로에서는 발생하지 않아야 함)
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    protected ResponseEntity<ApiResponse<?>> handleOptimisticLockingFailureException(
            final ObjectOptimisticLockingFailureException e) {
        log.error("handleOptimisticLockingFailureException : {}", e.getMessage());
        return new ResponseEntity<>(ApiResponse.error(ProposalErrorCode.EDIT_CONFLICT), HttpStatus.CONFLICT);
    }

}