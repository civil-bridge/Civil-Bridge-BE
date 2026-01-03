package org.example.gyeonggi_partners.domain.discussionRoom.infra.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 페이징된 논의방 ID 목록과 총 개수를 담는 DTO
 * 
 * <p>결정사항 5-3: 페이지네이션이므로 총 개수 반환 필요</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionRoomsPage {
    
    /**
     * 논의방 ID 목록
     */
    private List<Long> roomIds;
    
    /**
     * 전체 논의방 수
     * ZCARD 명령어로 조회한 값
     */
    private long totalRoomsCount;
    
    /**
     * 전체 페이지 수
     */
    public int getTotalPages(int pageSize) {
        return (int) Math.ceil((double) totalRoomsCount / pageSize);
    }
    
    /**
     * 현재 페이지가 마지막 페이지인지 확인
     */
    public boolean isLastPage(int currentPage, int pageSize) {
        return currentPage >= getTotalPages(pageSize) - 1;
    }
}
