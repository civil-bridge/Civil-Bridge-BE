package org.example.gyeonggi_partners.domain.discussionRoom.infra.cache;

import org.example.gyeonggi_partners.domain.discussionRoom.domain.model.DiscussionRoom;
import org.example.gyeonggi_partners.domain.discussionRoom.infra.cache.dto.DiscussionRoomCacheModel;
import org.example.gyeonggi_partners.domain.discussionRoom.infra.cache.dto.DiscussionRoomsPage;

import java.util.List;
import java.util.Optional;

/**
 * 논의방 Redis 캐시 Repository 인터페이스
 * 
 * <p>Redis를 활용한 캐싱 전략:</p>
 * <ul>
 *   <li>Read: Cache-Aside 패턴 (캐시 미스 시 DB 조회)</li>
 *   <li>Write: Write-Through 패턴 (DB 저장 후 캐시 업데이트)</li>
 *   <li>원자성: Redis Transaction (MULTI/EXEC) 사용</li>
 *   <li>TTL: room:{id} 24시간, list:latest 1시간, user:{id}:joined 12시간</li>
 * </ul>
 */
public interface DiscussionRoomCacheRepository {
    
    // ==================== 메인 시나리오 ====================
    
    /**
     * 논의방 생성 시 Redis 캐시 업데이트 (원자적 처리)
     * 
     * <p>결정사항 9: Redis Transaction (MULTI/EXEC) 사용</p>
     * 
     * <p>다음 3가지 작업을 원자적으로 수행:</p>
     * <ol>
     *   <li>room:{id} Hash 저장 (TTL: 24시간)</li>
     *   <li>list:latest ZSet 업데이트 (TTL: 1시간)</li>
     *   <li>user:{creatorId}:joined ZSet 업데이트 (TTL: 12시간)</li>
     * </ol>
     * 
     * @param cachedRoom 캐시할 논의방 정보
     * @param creatorId 생성자(방장) ID
     * @param timestamp 생성 시각 (밀리초 단위)
     */
    void saveNewRoomToRedis(DiscussionRoomCacheModel cachedRoom, Long creatorId, long timestamp);
    
    /**
     * 전체 논의방 최신순 목록 조회 (페이징)
     * 
     * <p>결정사항 5-2: Offset 기반 페이징</p>
     * <p>결정사항 5-3: 총 개수 반환 (페이지네이션)</p>
     * 
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기 (결정사항 5-1: 15개)
     * @return 페이징된 논의방 ID 목록 + 총 개수
     */
    DiscussionRoomsPage retrieveTotalRoomsByPage(int page, int size);
    
    /**
     * 사용자가 참여한 논의방 목록 조회 (최신 참여순, 페이징)
     * 
     * <p>결정사항 5-4: list:latest와 동일한 방식</p>
     * 
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 논의방 ID 목록 + 총 개수
     */
    DiscussionRoomsPage retrieveJoinedRoomsByPage(Long userId, int page, int size);
    
    /**
     * 사용자가 논의방에 입장 (원자적 처리)
     * 
     * <p>다음 3가지 작업을 원자적으로 수행:</p>
     * <ol>
     *   <li>user:{userId}:joined ZSet에 roomId 추가</li>
     *   <li>room:{roomId}:members List에 userId 추가</li>
     *   <li>room:{roomId} currentUsers 증가 (HINCRBY)</li>
     * </ol>
     * 
     * @param userId 사용자 ID
     * @param roomId 논의방 ID
     * @param timestamp 입장 시각 (밀리초)
     */
    void addUserToRoom(Long userId, Long roomId, long timestamp);
    
    /**
     * 논의방 삭제 시 캐시 무효화 (비동기)
     * 
     * <p>결정사항 4-1: 1~3번 모두 삭제</p>
     * <p>결정사항 4-2: 비동기 삭제</p>
     * <p>결정사항 4-3: 즉시 제거</p>
     * 
     * <p>삭제 범위:</p>
     * <ol>
     *   <li>room:{roomId} 삭제</li>
     *   <li>room:{roomId}:members 삭제</li>
     *   <li>list:latest에서 제거</li>
     *   <li>user:{creatorId}:joined에서 제거</li>
     * </ol>
     * 
     * @param roomId 논의방 ID
     * @param creatorId 생성자(방장) ID
     */
    void evictRoomCache(Long roomId, Long creatorId);
    
    // ==================== 헬퍼 / 내부 함수 ====================
    
    /**
     * 논의방 정보 조회 (room:{id})
     * 캐시 미스 시 DB에서 조회하여 캐싱 후 반환
     * 
     * @param roomId 논의방 ID
     * @return 캐시된 논의방 정보 (없으면 Empty)
     */
    Optional<DiscussionRoomCacheModel> retrieveCachingRoom(Long roomId);
    
    /**
     * 여러 논의방 정보 일괄 조회 (전체조회시 사용)
     * 
     * @param roomIds 논의방 ID 목록
     * @return 캐시된 논의방 목록 (캐시 미스는 제외)
     */
    List<DiscussionRoomCacheModel> retrieveTotalCachingRoom(List<Long> roomIds);
    
    /**
     * 사용자가 논의방에서 퇴장 (원자적 처리)
     * 
     * <p>다음 3가지 작업을 원자적으로 수행:</p>
     * <ol>
     *   <li>user:{userId}:joined ZSet에서 roomId 제거</li>
     *   <li>room:{roomId}:members List에서 userId 제거</li>
     *   <li>room:{roomId} currentUsers 감소 (HINCRBY -1)</li>
     * </ol>
     * 
     * @param userId 사용자 ID
     * @param roomId 논의방 ID
     */
    void removeUserFromRoom(Long userId, Long roomId);
    
    /**
     * 논의방 멤버 목록 조회
     * 
     * <p>결정사항 6-1: List 자료구조 사용</p>
     * 
     * @param roomId 논의방 ID
     * @return 멤버 ID 목록 (캐시 미스 시 빈 리스트)
     */
    List<Long> retrieveRoomMembers(Long roomId);
    
    /**
     * 논의방 멤버 목록 캐싱
     * 
     * @param roomId 논의방 ID
     * @param memberIds 멤버 ID 목록
     */
    void cacheRoomMembers(Long roomId, List<Long> memberIds);
    
    /**
     * list:latest ZSet 크기 제한
     * 
     * <p>결정사항 11-1: 최대 10,000개 유지</p>
     * <p>결정사항 11-2: 매번 추가 시 자동 제거</p>
     */
    void limitLatestSize();
    
    /**
     * user:{userId}:joined ZSet 크기 제한
     * 
     * <p>결정사항 11-2: 최대 100개 유지</p>
     */
    void limitUserJoinedList(Long userId);
    
    /**
     * 서버 시작 시 캐시 워밍
     * 
     * <p>결정사항 15-1: 최근 100개 방 미리 캐싱</p>
     * 
     * @param rooms 캐싱할 논의방 목록
     */
    void warmUpCache(List<DiscussionRoom> rooms, List<Integer> currentUsersCounts);
}
