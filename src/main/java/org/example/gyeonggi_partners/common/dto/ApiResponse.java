package org.example.gyeonggi_partners.common.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.gyeonggi_partners.common.exception.ErrorCode;

/**
 * API 응답을 위한 표준 래퍼 클래스 (Record)
 * @param code    애플리케이션 고유 응답 코드
 * @param message 응답 메시지
 * @param data    실제 응답 데이터
 * @param <T>     응답 데이터의 타입
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // data가 null일 경우 JSON에서 제외
public record ApiResponse<T>(
        String code,
        String message,
        T data
) {

    /**
     * 성공 응답을 생성합니다. 데이터와 메시지를 포함합니다.
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>("SUCCESS", message, data);
    }

    /**
     * 성공 응답을 생성합니다. 데이터만 포함합니다.
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "성공", data);
    }

    /**
     * 실패 응답을 생성합니다. ErrorCode를 사용합니다.
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 실패 응답을 생성합니다. ErrorCode와 추가 메시지를 사용합니다.
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null);
    }
}
