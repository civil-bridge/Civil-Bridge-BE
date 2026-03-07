package org.example.civilbridge.domain.proposal.domain.model;

public enum SubmitStatus {

    COMPLETED,      // 투표 완료 (가결)
    REJECTED,       // 투표 부결 (기한 만료 또는 정족수 미달)
    UNSUBMITTABLE,  // 제출 불가능 (편집 중)
    VOTING;         // 투표 중
}
