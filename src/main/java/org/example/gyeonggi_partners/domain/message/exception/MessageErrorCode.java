package org.example.gyeonggi_partners.domain.message.exception;


import org.example.gyeonggi_partners.common.exception.ErrorCode;


public enum MessageErrorCode implements ErrorCode {


    MESSAGE_CONTENT_EMPTY(400, "M001", "메시지 내용을 입력해주세요."),
    MESSAGE_TOO_LONG(400, "M002", "메시지는 2000자 이하로 입력해주세요."),
    INTERNAL_SERVER_ERROR(500, "COMMON500", "예기치 못 한 서버 에러입니다."),
    MESSAGE_USER_INCOINSISTENCY(400, "M003", "세션에 저장된 유저와 메세지를 보낸 유저가 일치하지 않습니다.")

    ;

    MessageErrorCode (int status, String code, String message){
        this.status = status;
        this.code = code;
        this.message = message;
    }

    private final int status;
    private final String code;
    private final String message;

    @Override
    public int getStatus() {
        return this.status;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
