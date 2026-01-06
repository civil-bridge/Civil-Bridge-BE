package org.example.civilbridge.domain.discussionRoom.infra.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.civilbridge.domain.discussionRoom.domain.model.AccessLevel;
import org.example.civilbridge.domain.discussionRoom.domain.model.DiscussionRoom;
import org.example.civilbridge.domain.discussionRoom.domain.model.Region;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 캐시에 저장될 논의방 데이터 전송 객체
 * 
 * <p>결정사항 1-1에 따라 다음 필드만 캐싱:</p>
 * <ul>
 *   <li>id, title,description,region, accessLevel, createdAt</li>
 *   <li>currentUsers (1-2 결정: 추가)</li>
 *   <li>description 제외 (메모리 절약)</li>
 *   <li>deletedAt 제외 (삭제된 방은 캐시에서 즉시 제거)</li>
 * </ul>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionRoomCacheModel {
    
    private Long id;
    private String title;
    private String description;      // 입장 시 필요 (추가)
    private Region region;
    private AccessLevel accessLevel;
    private LocalDateTime createdAt;
    private Integer currentUsers;
    
    /**
     * 도메인 모델을 캐시 DTO로 변환
     * 
     * @param domain 논의방 도메인 모델
     * @param currentUsers 현재 참여 인원 수 (DB에서 COUNT한 값)
     * @return 캐시용 DTO
     */
    public static DiscussionRoomCacheModel fromDomainModel(DiscussionRoom domain, int currentUsers) {
        return DiscussionRoomCacheModel.builder()
                .id(domain.getId())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .region(domain.getRegion())
                .accessLevel(domain.getAccessLevel())
                .createdAt(domain.getCreatedAt())
                .currentUsers(currentUsers)
                .build();
    }
    
    /**
     * 캐시 DTO를 도메인 모델로 변환
     * 
     * @return 논의방 도메인 모델
     */
    public DiscussionRoom ToDomainModel() {
        return DiscussionRoom.restore(
                this.id,
                this.title,
                this.description,
                this.region,
                this.accessLevel,
                this.createdAt,
                null,  // updatedAt은 캐시에 없음
                null   // deletedAt은 캐시에 없음
        );
    }
    
    /**
     * Redis Hash에 저장하기 위한 Map 변환
     * Enum과 LocalDateTime을 String으로 변환하여 저장
     * 
     * @return Hash 필드-값 맵
     */
    public Map<String, String> toRedisHash() {
        Map<String, String> map = new HashMap<>();
        map.put("id", String.valueOf(this.id));
        map.put("title", this.title);
        map.put("description", this.description != null ? this.description : "");
        map.put("region", this.region.name());
        map.put("accessLevel", this.accessLevel.name());
        map.put("createdAt", this.createdAt.toString());
        map.put("currentUsers", String.valueOf(this.currentUsers));
        return map;
    }
    
    /**
     * Redis Hash에서 가져온 Map을 DTO로 변환
     * String을 Enum과 LocalDateTime으로 변환
     * 
     * @param map Redis Hash 데이터
     * @return 캐시용 DTO
     */
    public static DiscussionRoomCacheModel fromRedisHash(Map<Object, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        
        return DiscussionRoomCacheModel.builder()
                .id(Long.valueOf(map.get("id").toString()))
                .title(map.get("title").toString())
                .description(map.get("description").toString())
                .region(Region.valueOf(map.get("region").toString()))
                .accessLevel(AccessLevel.valueOf(map.get("accessLevel").toString()))
                .createdAt(LocalDateTime.parse(map.get("createdAt").toString()))
                .currentUsers(Integer.valueOf(map.get("currentUsers").toString()))
                .build();
    }
}
