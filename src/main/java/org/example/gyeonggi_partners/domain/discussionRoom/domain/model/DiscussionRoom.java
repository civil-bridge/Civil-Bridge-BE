package org.example.gyeonggi_partners.domain.discussionRoom.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;


/**
 * DiscussionRoom 도메인 모델
 * JPA와 독립적인 순수 도메인 객체
 */
@Getter
public class DiscussionRoom {

    // 방 생성시 입력 규칙들

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 255;
    private static final String REGION_REQUIRED_MESSAGE = "지역은 반드시 입력해야합니다.";
    private static final String ACCESS_LEVEL_REQUIRED_MESSAGE = "입장 조건은 반드시 입력해야합니다.";


    private Long id;
    private String title;           // 논의방 제목
    private String description;     // 논의방 설명
    private Region region;          // 지역 (Region ENUM)
    private AccessLevel accessLevel;     // 접근 권한 (AccessLevel ENUM)// 참여 인원 수
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @Builder(access = lombok.AccessLevel.PRIVATE)
    private DiscussionRoom(Long id, String title, String description,
                           Region region, AccessLevel accessLevel,
                           LocalDateTime createdAt, LocalDateTime updatedAt,
                           LocalDateTime deletedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.region = region;
        this.accessLevel = accessLevel;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    /**
     * 새로운 논의방 생성
     */
    public static DiscussionRoom create(String title, String description,
                                        Region region, AccessLevel accessLevel) {
        validateTitle(title);
        validateDescription(description);
        validateRegion(region);
        validateAccessLevel(accessLevel);

        return DiscussionRoom.builder()
                .title(title)
                .description(description)
                .region(region)
                .accessLevel(accessLevel)// 생성자가 첫 멤버
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 기존 논의방 복원 (DB에서 조회)
     */
    public static DiscussionRoom restore(Long id, String title, String description,
                                         Region region, AccessLevel accessLevel,
                                         LocalDateTime createdAt, LocalDateTime updatedAt,
                                         LocalDateTime deletedAt) {
        return DiscussionRoom.builder()
                .id(id)
                .title(title)
                .description(description)
                .region(region)
                .accessLevel(accessLevel)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .build();
    }

    // ==================== Validation Methods ====================

    private static void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("논의방 제목은 필수입니다.");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                String.format("논의방 제목은 %d자를 초과할 수 없습니다.", MAX_TITLE_LENGTH));
        }
    }

    private static void validateDescription(String description) {
        // description은 선택사항이므로 null 허용
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                String.format("논의방 설명은 %d자를 초과할 수 없습니다.", MAX_DESCRIPTION_LENGTH));
        }
    }

    private static void validateRegion(Region region) {
        if (region == null) {
            throw new IllegalArgumentException(REGION_REQUIRED_MESSAGE);
        }

    }

    private static void validateAccessLevel(AccessLevel accessLevel) {
        if(accessLevel == null) {
            throw new IllegalArgumentException(ACCESS_LEVEL_REQUIRED_MESSAGE);
        }
    }
}
