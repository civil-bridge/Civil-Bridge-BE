package org.example.gyeonggi_partners.domain.discussionRoom.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.gyeonggi_partners.domain.discussionRoom.domain.model.AccessLevel;
import org.example.gyeonggi_partners.domain.discussionRoom.domain.model.Region;
import org.example.gyeonggi_partners.domain.discussionRoom.infra.cache.dto.DiscussionRoomCacheModel;

import java.time.LocalDateTime;

/**
 * 논의방 요약 정보 응답 DTO
 * 전체 목록 조회 및 내가 참여한 방 조회 시 사용
 */
@Getter
@Builder
@Schema(description = "논의방 요약 정보")
public class DiscussionRoomInfo {
    
    @Schema(description = "논의방 ID", example = "1")
    private Long roomId;
    
    @Schema(description = "논의방 제목", example = "부천시 BJ로 인한 지역 상권문제")
    private String title;

    @Schema(description = "지역", example = "BUCHEON")
    private Region region;
    
    @Schema(description = "입장 조건", example = "PUBLIC")
    private AccessLevel accessLevel;
    
    @Schema(description = "현재 참여 인원", example = "15")
    private Integer currentUsers;
    
    @Schema(description = "생성 일시", example = "2025-11-06T10:30:00")
    private LocalDateTime createdAt;
    
    /**
     * CachedDiscussionRoom에서 응답 DTO로 변환
     * 
     * @param cached 캐시된 논의방 정보
     * @return 논의방 요약 정보 응답 DTO
     */
    public static DiscussionRoomInfo from(DiscussionRoomCacheModel cached) {
        return DiscussionRoomInfo.builder()
                .roomId(cached.getId())
                .title(cached.getTitle())// 캐시에는 description이 없음
                .region(cached.getRegion())
                .accessLevel(cached.getAccessLevel())
                .currentUsers(cached.getCurrentUsers())
                .createdAt(cached.getCreatedAt())
                .build();
    }
}
