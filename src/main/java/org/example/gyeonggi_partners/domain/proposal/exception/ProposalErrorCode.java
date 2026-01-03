package org.example.gyeonggi_partners.domain.proposal.exception;

import org.example.gyeonggi_partners.common.exception.ErrorCode;

public enum ProposalErrorCode implements ErrorCode {

    /**
     * 더 정의하거나 삭제해야할 상수 있는지 체크해볼것.
     * */

    // Enum 상수 정의
    PROPOSAL_NOT_FOUND(404, "P001", "존재하지 않는 제안서입니다."),
    PROPOSAL_BEING_EDITED(409, "P002", "다른 사용자가 현재 제안서를 수정 중입니다."),
    ALREADY_CONSENTED(409, "P003", "이미 동의한 제안서입니다."),
    PROPOSAL_LOCKED(409, "P004", "현재 동의가 진행 중인 제안서는 수정할 수 없습니다."),
    EDIT_CONFLICT(409, "P005", "다른 사용자에 의해 문서가 수정되었습니다. 페이지를 새로고침 해주세요."),

    // 유효하지 않은 client 요청
    INVALID_TITLE_LENGTH(400, "P006", "제목의 길이가 올바르지 않습니다."),
    EMPTY_PROPOSAL_BODY(400, "P007", "제안서 본문 내용이 없습니다."),

    UNAUTHORIZED_ACCESS(403, "P008", "해당 제안서에 대한 권한이 없습니다."),
    NOT_IN_VOTING(400, "P009", "투표 중인 제안서가 아닙니다."),
    ALREADY_VOTING(409, "P012", "이미 투표가 진행 중입니다."),
    ALREADY_SUBMITTABLE(409, "P013", "이미 제출 가능한 상태입니다."),
    INVALID_VOTING_PERIOD(400, "P014", "투표 기간은 1일 이상 30일 이하여야 합니다."),
    VOTING_DEADLINE_EXPIRED(400, "P015", "투표 마감 시간이 지났습니다."),
    PROPOSAL_LIMIT_EXCEEDED(400, "P016", "논의방당 최대 5개의 제안서만 생성할 수 있습니다.");

    private final int status;
    private final String code;
    private final String message;

    ProposalErrorCode (int status, String code, String message){
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public int getStatus() {
        return this.status; // 0 대신 현재 객체의 status 필드를 반환합니다.
    }

    @Override
    public String getCode() { // ErrorCode 인터페이스에 getCode()가 있다고 가정
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message; // "" 대신 현재 객체의 message 필드를 반환합니다.
    }

}
