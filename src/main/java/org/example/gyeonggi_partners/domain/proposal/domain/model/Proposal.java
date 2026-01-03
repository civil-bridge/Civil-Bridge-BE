package org.example.gyeonggi_partners.domain.proposal.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Proposal {

    // 제목 길이 제한
    public static final int MIN_TITLE_LENGTH = 5;
    public static final int MAX_TITLE_LENGTH = 100;
    // 마감기간, 최소 동의 인원
    public static final int SUBMISSION_DURATION_DAYS = 3;
    public static final int SUBMISSION_MIN_CONSENTS_COUNT = 10;


    // Id - 제안서, 논의방, 작성자
    private Long id;
    private Long roomId;
    // 제안서 - 제목, 내용
    private String title;
    private ContentFormat contents;

    // 제출 - 제출 상태, 동의자 목록, 마감 날짜
    private SubmitStatus status;
    private List<Consenter> consents;
    private LocalDateTime deadline;

    // 날짜 - 생성, 수정, 삭제
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;


    public static Proposal create(Long roomId,String title, ContentFormat contents) {

        validateTitle(title);

        return  Proposal.builder()
                .roomId(roomId)
                .title(title)
                .contents(contents)
                .status(SubmitStatus.UNSUBMITTABLE)
                .consents(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

    }


    public void update(String title, ContentFormat contents) {
        validateTitle(title);

        this.title = title;
        this.contents = contents;
        this.updatedAt = LocalDateTime.now();
    }



    // 투표 시작
    public void startVoting() {

        if (status == SubmitStatus.VOTING) {
            throw new IllegalArgumentException("이미 투표가 진행 중입니다.");
        }

        if (status == SubmitStatus.SUBMITTABLE) {
            throw new IllegalArgumentException("이미 제출 가능한 상태입니다");
        }

        this.status = SubmitStatus.VOTING;
        this.deadline = LocalDateTime.now().plusDays(SUBMISSION_DURATION_DAYS);
    }

    /**
     * 투표 종료
     */
    public void endVoting() {
        if(this.status != SubmitStatus.VOTING) {
            throw new IllegalArgumentException("투표 중인 제안서가 아닙니다.");
        }

        int consentCount = this.consents != null ? this.consents.size() : 0;

        if (consentCount >= SUBMISSION_MIN_CONSENTS_COUNT) {
            this.status = SubmitStatus.SUBMITTABLE;
        } else {
            this.status = SubmitStatus.UNSUBMITTABLE;
        }

        this.deadline = null;
    }

    /**
     * 투표 기간 만료 확인 및 상태 업데이트
     */
    public void checkAndUpdateVotingStatus() {
        if(this.status != SubmitStatus.VOTING) {
            return;
        }

        if(this.deadline != null && LocalDateTime.now().isAfter(this.deadline)) {
            endVoting();
        }
    }


    // 동의 하기
    public void addConsent(Consenter consenter) {
        if (this.consents == null) {
            this.consents = new ArrayList<>();
        }

        if (this.status != SubmitStatus.VOTING) {
            throw new IllegalArgumentException("투표 중인 제안서가 아닙니다");
        }

        if (this.deadline != null && LocalDateTime.now().isAfter(this.deadline)) {
            throw new IllegalArgumentException("투표 마감 시간이 지난 제안서입니다.");
        }


        boolean alreadyConsented = this.consents.stream()
                .anyMatch(u -> u.getId().equals(consenter.getId()));

        if(alreadyConsented) {
            throw new IllegalArgumentException("이미 동의한 제안서입니다");
        }

        this.consents.add(consenter);

    }

    // 마감기간,인원 충족 여부 확인
    public void checkSubmitStatus(int roomMemberCount) {

        if (status != SubmitStatus.VOTING) {
            throw new IllegalArgumentException("제출 중인 상태가 아닙니다");
        }


        if(LocalDateTime.now().isAfter(deadline)) {
            this.status = SubmitStatus.UNSUBMITTABLE;
        }

        if (LocalDateTime.now().isBefore(deadline)) {

            if (consents.size() >= SUBMISSION_MIN_CONSENTS_COUNT) {
                this.status = SubmitStatus.SUBMITTABLE;
            }
        }
    }

    public static Proposal restore(Long id, Long roomId, String title, ContentFormat contents,
                            List<Consenter> consents, SubmitStatus status, LocalDateTime deadline, LocalDateTime createdAt,
                            LocalDateTime updatedAt, LocalDateTime deletedAt) {
        return Proposal.builder()
                .id(id)
                .roomId(roomId)
                .title(title)
                .contents(contents)
                .consents(consents)
                .status(status)
                .deadline(deadline)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .build();
    }


    private static void validateTitle(String title) {
        if(title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("제안서 제목은 필수입니다");
        }

        if (title.length() < MIN_TITLE_LENGTH || title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("제목 길이는 5자 이상 100자 이하이어야 합니다.");
        }
    }

}
