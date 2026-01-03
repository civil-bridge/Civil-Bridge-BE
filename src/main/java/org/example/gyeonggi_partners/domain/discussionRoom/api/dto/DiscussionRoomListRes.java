package org.example.gyeonggi_partners.domain.discussionRoom.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 논의방 목록 응답 DTO
 * 페이징 정보 포함
 */
@Getter
@Builder
@Schema(description = "논의방 목록 응답")
public class DiscussionRoomListRes {
    
    @Schema(description = "논의방 목록")
    private List<DiscussionRoomInfo> rooms;
    
    @Schema(description = "현재 페이지", example = "1")
    private int currentPage;
    
    @Schema(description = "페이지 크기", example = "15")
    private int pageSize;
    
    @Schema(description = "전체 논의방 개수", example = "150")
    private long totalCount;
    
    @Schema(description = "전체 페이지 수", example = "10")
    private int totalPages;
    
    /**
     * 논의방 목록과 페이징 정보로 응답 DTO 생성
     * 
     * @param rooms 논의방 목록
     * @param page 현재 페이지
     * @param size 페이지 크기
     * @param totalCount 전체 개수
     * @return 논의방 목록 응답 DTO
     */
    public static DiscussionRoomListRes of(
            List<DiscussionRoomInfo> rooms,
            int page,
            int size,
            long totalCount
    ) {
        return DiscussionRoomListRes.builder()
                .rooms(rooms)
                .currentPage(page)
                .pageSize(size)
                .totalCount(totalCount)
                .totalPages((int) Math.ceil((double) totalCount / size))
                .build();
    }
}
