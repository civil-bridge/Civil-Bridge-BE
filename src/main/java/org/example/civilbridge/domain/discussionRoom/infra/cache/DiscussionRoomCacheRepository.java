package org.example.civilbridge.domain.discussionRoom.infra.cache;

import org.example.civilbridge.domain.discussionRoom.infra.cache.dto.DiscussionRoomCacheModel;

import java.util.List;
import java.util.Map;
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

    /**
     * 논의방 생성 시 Redis 캐시 업데이트 (원자적 처리)
     *
     * <p>다음 4가지 작업을 원자적으로 수행:</p>
     * <ol>
     *   <li>room:{id} Hash 저장 (TTL: 24시간)</li>
     *   <li>list:latest ZSet 업데이트 (TTL: 1시간)</li>
     *   <li>user:{creatorId}:joined ZSet 업데이트 (TTL: 12시간)</li>
     *   <li>room:{id}:members List에 생성자 추가 (TTL: 24시간)</li>
     * </ol>
     *
     * @param cachedRoom 캐시할 논의방 정보
     * @param creatorId 생성자(방장) ID
     * @param timestamp 생성 시각 (밀리초 단위)
     */
    void saveNewRoomToRedis(DiscussionRoomCacheModel cachedRoom, Long creatorId, long timestamp);

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
     * 논의방 삭제 시 캐시 무효화
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

    /**
     * 논의방 정보 조회 (캐시 미스 시 DB에서 조회하여 캐싱 후 반환)
     *
     * @param roomId 논의방 ID
     * @return 캐시된 논의방 정보 (존재하지 않으면 Empty)
     */
    Optional<DiscussionRoomCacheModel> retrieveCachingRoom(Long roomId);

    /**
     * 여러 논의방 정보를 Redis Pipeline으로 일괄 조회 (DB fallback 없음)
     * 하나의 왕복으로 N개의 HGETALL을 실행한다.
     *
     * @param roomIds 조회할 논의방 ID 목록
     * @return roomId → Optional<캐시모델> 맵 (캐시 미스 방은 Optional.empty())
     */
    Map<Long, Optional<DiscussionRoomCacheModel>> getCachedRoomsAll(List<Long> roomIds);

    /**
     * 논의방 정보를 Redis에 캐싱 (room:{id} Hash만)
     *
     * @param model 캐싱할 논의방 모델
     */
    void cacheRoomInfo(DiscussionRoomCacheModel model);

    /**
     * 논의방 멤버 목록 조회
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
}
