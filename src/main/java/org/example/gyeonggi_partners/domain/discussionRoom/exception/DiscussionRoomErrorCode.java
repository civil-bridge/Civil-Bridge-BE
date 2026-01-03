package org.example.gyeonggi_partners.domain.discussionRoom.exception;

import org.example.gyeonggi_partners.common.exception.ErrorCode;

public enum DiscussionRoomErrorCode implements ErrorCode {

    ROOM_NOT_FOUND(404, "R001", "존재하지 않는 논의방입니다."),
    OFFICIALS_ONLY_ROOM(403, "R002", "이 논의방은 공무원만 참여할 수 있습니다."),
    USERS_ONLY_ROOM(403, "R003", "이 논의방은 일반 사용자만 참여할 수 있습니다."),
    REGION_MISMATCH(403, "R003", "해당 지역 주민만 참여할 수 있는 논의방입니다."),
    ALREADY_JOINED_ROOM(409, "R005", "이미 참여 중인 논의방입니다."),
    NOT_A_ROOM_MEMBER(403, "R006", "해당 논의방의 멤버가 아닙니다.");
    DiscussionRoomErrorCode (int status, String code, String message){
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
