package org.example.gyeonggi_partners.domain.discussionRoom.domain.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Member 도메인 모델
 * 사용자와 논의방의 참여 관계를 나타내는 순수 도메인 객체
 */
@Getter
public class Member {

    private Long id;
    private Long userId;        // 참여한 사용자 ID
    private Long roomId;        // 참여한 논의방 ID
    private LocalDateTime createdAt;  // 참여 시각

    @Builder(access = AccessLevel.PRIVATE)
    private Member(Long id, Long userId, Long roomId, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.roomId = roomId;
        this.createdAt = createdAt;
    }

    /**
     * 새로운 멤버 생성 (논의방 참여)
     */
    public static Member join(Long userId, Long roomId) {
        validateUserId(userId);
        validateRoomId(roomId);

        return Member.builder()
                .userId(userId)
                .roomId(roomId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 기존 멤버 복원 (DB에서 조회)
     */
    public static Member restore(Long id, Long userId, Long roomId, LocalDateTime createdAt) {
        return Member.builder()
                .id(id)
                .userId(userId)
                .roomId(roomId)
                .createdAt(createdAt)
                .build();
    }

    // ==================== Validation Methods ====================

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
    }

    private static void validateRoomId(Long roomId) {
        if (roomId == null) {
            throw new IllegalArgumentException("논의방 ID는 필수입니다.");
        }
    }
}
