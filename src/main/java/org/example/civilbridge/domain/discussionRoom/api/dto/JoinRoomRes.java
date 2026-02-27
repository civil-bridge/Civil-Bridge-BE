package org.example.civilbridge.domain.discussionRoom.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.civilbridge.domain.discussionRoom.domain.model.AccessLevel;
import org.example.civilbridge.domain.discussionRoom.infra.cache.dto.DiscussionRoomCacheModel;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 논의방 입장 응답 DTO
 */
@Getter
@Builder
@Schema(description = "논의방 입장 응답")
public class JoinRoomRes {

    @Schema(description = "논의방 ID", example = "1")
    private Long roomId;

    @Schema(description = "논의방 제목", example = "부천시 BJ로 인한 지역 상권문제")
    private String title;

    @Schema(description = "논의방 설명", example = "현재 부천시 BJ로 인한 상권 문제에 대해 논의합니다")
    private String description;

    @Schema(description = "시/군", example = "부천시")
    private String city;

    @Schema(description = "구/동", example = "원미구")
    private String district;

    @Schema(description = "접근 범위", example = "PUBLIC")
    private AccessLevel accessLevel;

    @Schema(description = "현재 멤버 수", example = "15")
    private Integer currentUsers;

    @Schema(description = "멤버 목록")
    private List<MemberInfo> members;

    @Schema(description = "입장 시각", example = "2025-11-08T14:30:00")
    private LocalDateTime joinedAt;

    public static JoinRoomRes of(DiscussionRoomCacheModel cached, List<MemberInfo> members) {
        return JoinRoomRes.builder()
                .roomId(cached.getId())
                .title(cached.getTitle())
                .description(cached.getDescription())
                .city(cached.getCity())
                .district(cached.getDistrict())
                .accessLevel(cached.getAccessLevel())
                .currentUsers(cached.getCurrentUsers())
                .members(members)
                .joinedAt(LocalDateTime.now())
                .build();
    }
}
