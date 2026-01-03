package org.example.gyeonggi_partners.domain.discussionRoom.infra.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gyeonggi_partners.domain.discussionRoom.domain.model.DiscussionRoom;
import org.example.gyeonggi_partners.domain.discussionRoom.domain.repository.DiscussionRoomRepository;
import org.example.gyeonggi_partners.domain.discussionRoom.domain.repository.MemberRepository;
import org.example.gyeonggi_partners.domain.discussionRoom.infra.cache.dto.DiscussionRoomCacheModel;
import org.example.gyeonggi_partners.domain.discussionRoom.infra.cache.dto.DiscussionRoomsPage;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 논의방 Redis 캐시 Repository 구현체
 * 
 * <p>Redis 명령어 매핑:</p>
 * <ul>
 *   <li>Hash: HSET, HGETALL, HINCRBY, DEL</li>
 *   <li>ZSet: ZADD, ZREVRANGE, ZCARD, ZREM, ZREMRANGEBYRANK</li>
 *   <li>List: LPUSH, LRANGE, LREM, DEL</li>
 *   <li>Transaction: MULTI, EXEC</li>
 * </ul>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DiscussionRoomCacheRepositoryImpl implements DiscussionRoomCacheRepository {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final DiscussionRoomRepository discussionRoomRepository;
    private final MemberRepository memberRepository;
    
    // TTL 상수 (결정사항 8)
    private static final Duration TTL_ROOM_INFO = Duration.ofHours(24);      // 24시간
    private static final Duration TTL_RECENT_ROOMS = Duration.ofHours(1);       // 1시간
    private static final Duration TTL_USER_ROOM = Duration.ofHours(12);      // 12시간
    private static final Duration TTL_ROOM_MEMBERS = Duration.ofHours(24);     // 24시간 (room:{id}와 동일)
    
    // ZSet 크기 제한 상수 (결정사항 11)
    private static final int MAX_LATEST_LIST_SIZE = 10_000;
    private static final int MAX_USER_JOINED_SIZE = 100;
    
    // ==================== 메인 시나리오 ====================
    
    // 논의방 생성
    @Override
    public void saveNewRoomToRedis(DiscussionRoomCacheModel cachedRoom, Long creatorId, long timestamp) {
        try {
            //캐시 키 생성
            String roomInfoKey = RedisKeyGenerator.generateRoomInfoKey(cachedRoom.getId());
            String recentRoomsKey = RedisKeyGenerator.generateRecentRoomsKey();
            String userRoomsKey = RedisKeyGenerator.generateUserRoomsKey(creatorId);
            
            // 결정사항 9: Redis Transaction (MULTI/EXEC) 사용
            redisTemplate.execute(new SessionCallback<List<Object>>() {

                @Override
                @SuppressWarnings("unchecked")
                public List<Object> execute(RedisOperations operations) {
                    operations.multi();
                    
                    // 1. room:{id} Hash 저장
                    operations.opsForHash().putAll(roomInfoKey, cachedRoom.toRedisHash());
                    operations.expire(roomInfoKey, TTL_ROOM_INFO);
                    
                    // 2. list:latest ZSet 업데이트 (결정사항 2-2: 최신순)
                    operations.opsForZSet().add(recentRoomsKey, cachedRoom.getId(), (double) timestamp);
                    operations.expire(recentRoomsKey, TTL_RECENT_ROOMS);
                    
                    // 3. user:{creatorId}:joined ZSet 업데이트
                    operations.opsForZSet().add(userRoomsKey, cachedRoom.getId(), (double) timestamp);
                    operations.expire(userRoomsKey, TTL_USER_ROOM);
                    
                    return operations.exec();
                }
            });
            
            // 결정사항 11-1: list:latest 크기 제한 (매번 추가 시)
            limitLatestSize();
            
            // 결정사항 11-2: user:joined 크기 제한
            limitUserJoinedList(creatorId);
            
            log.debug("캐시 저장 성공 - room:{}, creator:{}", cachedRoom.getId(), creatorId);
            
        } catch (Exception e) {
            // 결정사항 10-2: Redis 실패 시 로그만 남기고 계속 진행
            log.error("캐시 저장 실패 - room:{}, error: {}", cachedRoom.getId(), e.getMessage(), e);
        }
    }
    
    // 전체 논의방 목록 조회 (페이징)
    @Override
    public DiscussionRoomsPage retrieveTotalRoomsByPage(int page, int size) {
        try {
            String latestKey = RedisKeyGenerator.generateRecentRoomsKey();
            
            // 결정사항 5-3: 페이지네이션이므로 총 개수 반환
            Long totalCount = redisTemplate.opsForZSet().zCard(latestKey);
            if (totalCount == null || totalCount == 0) {
                return DiscussionRoomsPage.builder()
                        .roomIds(Collections.emptyList())
                        .totalRoomsCount(0)
                        .build();
            }
            
            // 결정사항 5-2: Offset 기반 페이징
            // 결정사항 2-2: 최신순 정렬 (ZREVRANGE - Score 큰 값부터)
            int start = page * size;
            int end = start + size - 1;
            
            Set<Object> roomIdsSet = redisTemplate.opsForZSet()
                    .reverseRange(latestKey, start, end);
            
            if (roomIdsSet == null || roomIdsSet.isEmpty()) {
                return DiscussionRoomsPage.builder()
                        .roomIds(Collections.emptyList())
                        .totalRoomsCount(totalCount)
                        .build();
            }
            
            List<Long> roomIds = roomIdsSet.stream()
                    .map(obj -> Long.valueOf(obj.toString()))
                    .collect(Collectors.toList());
            
            log.debug("전체 목록 조회 - page:{}, size:{}, total:{}, found:{}", 
                    page, size, totalCount, roomIds.size());
            
            return DiscussionRoomsPage.builder()
                    .roomIds(roomIds)
                    .totalRoomsCount(totalCount)
                    .build();
            
        } catch (Exception e) {
            log.error("전체 목록 조회 실패 - page:{}, size:{}, error: {}", 
                    page, size, e.getMessage(), e);
            return DiscussionRoomsPage.builder()
                    .roomIds(Collections.emptyList())
                    .totalRoomsCount(0)
                    .build();
        }
    }
    
    // 사용자 참여 방 조회 (페이징)
    @Override
    public DiscussionRoomsPage retrieveJoinedRoomsByPage(Long userId, int page, int size) {
        try {
            String userJoinedKey = RedisKeyGenerator.generateUserRoomsKey(userId);
            
            // 총 개수 조회
            Long totalCount = redisTemplate.opsForZSet().zCard(userJoinedKey);
            if (totalCount == null || totalCount == 0) {
                return DiscussionRoomsPage.builder()
                        .roomIds(Collections.emptyList())
                        .totalRoomsCount(0)
                        .build();
            }
            
            // 페이징 (최신 참여순)
            int start = page * size;
            int end = start + size - 1;
            
            Set<Object> roomIdsSet = redisTemplate.opsForZSet()
                    .reverseRange(userJoinedKey, start, end);
            
            if (roomIdsSet == null || roomIdsSet.isEmpty()) {
                return DiscussionRoomsPage.builder()
                        .roomIds(Collections.emptyList())
                        .totalRoomsCount(totalCount)
                        .build();
            }
            
            List<Long> roomIds = roomIdsSet.stream()
                    .map(obj -> Long.valueOf(obj.toString()))
                    .collect(Collectors.toList());
            
            log.debug("사용자 참여 방 조회 - user:{}, page:{}, size:{}, total:{}, found:{}", 
                    userId, page, size, totalCount, roomIds.size());
            
            return DiscussionRoomsPage.builder()
                    .roomIds(roomIds)
                    .totalRoomsCount(totalCount)
                    .build();
            
        } catch (Exception e) {
            log.error("사용자 참여 방 조회 실패 - user:{}, page:{}, size:{}, error: {}", 
                    userId, page, size, e.getMessage(), e);
            return DiscussionRoomsPage.builder()
                    .roomIds(Collections.emptyList())
                    .totalRoomsCount(0)
                    .build();
        }
    }
    
    // 논의방 입장
    @Override
    public void addUserToRoom(Long userId, Long roomId, long timestamp) {
        try {
            String userJoinedKey = RedisKeyGenerator.generateUserRoomsKey(userId);
            String roomMembersKey = RedisKeyGenerator.generateRoomMembersKey(roomId);
            String roomKey = RedisKeyGenerator.generateRoomInfoKey(roomId);
            
            // Transaction으로 원자적 처리
            redisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                @SuppressWarnings("unchecked")
                public List<Object> execute(RedisOperations operations) {
                    operations.multi();
                    
                    // 1. user:{userId}:joined ZSet에 추가
                    operations.opsForZSet().add(userJoinedKey, roomId, (double) timestamp);
                    operations.expire(userJoinedKey, TTL_USER_ROOM);
                    
                    // 2. room:{roomId}:members List에 추가 (결정사항 6-1: List 사용)
                    operations.opsForList().rightPush(roomMembersKey, userId);
                    operations.expire(roomMembersKey, TTL_ROOM_MEMBERS);
                    
                    // 3. room:{roomId} currentUsers 증가 (결정사항 14-2: HINCRBY)
                    operations.opsForHash().increment(roomKey, "currentUsers", 1);
                    
                    return operations.exec();
                }
            });
            
            // user:joined 크기 제한
            limitUserJoinedList(userId);
            
            log.debug("논의방 입장 - user:{}, room:{}", userId, roomId);
            
        } catch (Exception e) {
            log.error("논의방 입장 실패 - user:{}, room:{}, error: {}", 
                    userId, roomId, e.getMessage(), e);
        }
    }
    
    // 논의방 삭제
    @Override
    public void evictRoomCache(Long roomId, Long creatorId) {
        try {
            String roomKey = RedisKeyGenerator.generateRoomInfoKey(roomId);
            String roomMembersKey = RedisKeyGenerator.generateRoomMembersKey(roomId);
            String latestKey = RedisKeyGenerator.generateRecentRoomsKey();
            String userJoinedKey = RedisKeyGenerator.generateUserRoomsKey(creatorId);
            
            // 결정사항 4-1: 1~3번 모두 삭제
            // 결정사항 4-3: 즉시 제거
            redisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                @SuppressWarnings("unchecked")
                public List<Object> execute(RedisOperations operations) {
                    operations.multi();
                    
                    // 1. room:{roomId} 삭제
                    operations.delete(roomKey);
                    
                    // 2. room:{roomId}:members 삭제
                    operations.delete(roomMembersKey);
                    
                    // 3. list:latest에서 제거
                    operations.opsForZSet().remove(latestKey, roomId);
                    
                    // 4. user:{creatorId}:joined에서 제거
                    operations.opsForZSet().remove(userJoinedKey, roomId);
                    
                    return operations.exec();
                }
            });
            
            log.info("캐시 삭제 완료 - room:{}, creator:{}", roomId, creatorId);
            
        } catch (Exception e) {
            log.error("캐시 삭제 실패 - room:{}, creator:{}, error: {}", 
                    roomId, creatorId, e.getMessage(), e);
        }
    }
    
    // ==================== 헬퍼 메서드 ====================
    
    // 단일 논의방 조회 (캐시 미스 시 DB 조회 후 캐싱)
    @Override
    public Optional<DiscussionRoomCacheModel> retrieveCachingRoom(Long roomId) {
        try {
            String roomKey = RedisKeyGenerator.generateRoomInfoKey(roomId);
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(roomKey);
            
            if (entries.isEmpty()) {
                log.debug("캐시 미스 - DB 조회 시작 - room:{}", roomId);
                
                // DB에서 조회
                Optional<DiscussionRoom> roomOpt = discussionRoomRepository.findById(roomId);
                if (roomOpt.isEmpty()) {
                    log.debug("DB에도 존재하지 않음 - room:{}", roomId);
                    return Optional.empty();
                }
                
                // 현재 멤버 수 조회
                int currentUsers = memberRepository.countByRoomId(roomId);
                
                // 캐시 모델 생성
                DiscussionRoomCacheModel model = DiscussionRoomCacheModel.fromDomainModel(
                    roomOpt.get(), 
                    currentUsers
                );
                
                // Redis에 캐싱 (room:{id} Hash만 저장)
                redisTemplate.opsForHash().putAll(roomKey, model.toRedisHash());
                redisTemplate.expire(roomKey, TTL_ROOM_INFO);
                
                log.debug("DB 조회 및 캐싱 완료 - room:{}, currentUsers:{}", roomId, currentUsers);
                return Optional.of(model);
            }
            
            DiscussionRoomCacheModel cached = DiscussionRoomCacheModel.fromRedisHash(entries);
            log.debug("캐시 히트 - room:{}", roomId);
            return Optional.ofNullable(cached);
            
        } catch (Exception e) {
            log.error("캐시 조회 실패 - room:{}, error: {}", roomId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    // 여러 논의방 일괄 조회
    @Override
    public List<DiscussionRoomCacheModel> retrieveTotalCachingRoom(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<DiscussionRoomCacheModel> result = new ArrayList<>();
        
        for (Long roomId : roomIds) {
            retrieveCachingRoom(roomId).ifPresent(result::add);
        }
        
        return result;
    }
    
    // 논의방 퇴장
    @Override
    public void removeUserFromRoom(Long userId, Long roomId) {
        try {
            String userJoinedKey = RedisKeyGenerator.generateUserRoomsKey(userId);
            String roomMembersKey = RedisKeyGenerator.generateRoomMembersKey(roomId);
            String roomKey = RedisKeyGenerator.generateRoomInfoKey(roomId);
            
            // Transaction으로 원자적 처리
            redisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                @SuppressWarnings("unchecked")
                public List<Object> execute(RedisOperations operations) {
                    operations.multi();
                    
                    // 1. user:{userId}:joined ZSet에서 제거
                    operations.opsForZSet().remove(userJoinedKey, roomId);
                    
                    // 2. room:{roomId}:members List에서 제거
                    operations.opsForList().remove(roomMembersKey, 0, userId);
                    
                    // 3. room:{roomId} currentUsers 감소
                    operations.opsForHash().increment(roomKey, "currentUsers", -1);
                    
                    return operations.exec();
                }
            });
            
            log.debug("논의방 퇴장 - user:{}, room:{}", userId, roomId);
            
        } catch (Exception e) {
            log.error("논의방 퇴장 실패 - user:{}, room:{}, error: {}", 
                    userId, roomId, e.getMessage(), e);
        }
    }
    
    // 멤버 목록 조회
    @Override
    public List<Long> retrieveRoomMembers(Long roomId) {
        try {
            String roomMembersKey = RedisKeyGenerator.generateRoomMembersKey(roomId);
            
            // 결정사항 6-1: List 사용
            List<Object> members = redisTemplate.opsForList().range(roomMembersKey, 0, -1);
            
            if (members == null || members.isEmpty()) {
                log.debug("멤버 목록 캐시 미스 - room:{}", roomId);
                return Collections.emptyList();
            }
            
            List<Long> memberIds = members.stream()
                    .map(obj -> Long.valueOf(obj.toString()))
                    .collect(Collectors.toList());
            
            log.debug("멤버 목록 조회 - room:{}, count:{}", roomId, memberIds.size());
            return memberIds;
            
        } catch (Exception e) {
            log.error("멤버 목록 조회 실패 - room:{}, error: {}", roomId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    // 멤버 목록 캐싱
    @Override
    public void cacheRoomMembers(Long roomId, List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }
        
        try {
            String roomMembersKey = RedisKeyGenerator.generateRoomMembersKey(roomId);
            
            // 기존 데이터 삭제 후 재저장
            redisTemplate.delete(roomMembersKey);
            redisTemplate.opsForList().rightPushAll(roomMembersKey, memberIds.toArray());
            redisTemplate.expire(roomMembersKey, TTL_ROOM_MEMBERS);
            
            log.debug("멤버 목록 캐싱 - room:{}, count:{}", roomId, memberIds.size());
            
        } catch (Exception e) {
            log.error("멤버 목록 캐싱 실패 - room:{}, error: {}", roomId, e.getMessage(), e);
        }
    }
    
    // 전체 목록 크기 제한
    @Override
    public void limitLatestSize() {
        try {
            String latestKey = RedisKeyGenerator.generateRecentRoomsKey();
            Long count = redisTemplate.opsForZSet().zCard(latestKey);
            
            if (count != null && count > MAX_LATEST_LIST_SIZE) {
                // 오래된 것부터 제거 (Score 작은 것부터)
                long removeCount = count - MAX_LATEST_LIST_SIZE;
                redisTemplate.opsForZSet().removeRange(latestKey, 0, removeCount - 1);
                
                log.debug("list:latest 크기 제한 - 제거: {}개, 남은: {}개", 
                        removeCount, MAX_LATEST_LIST_SIZE);
            }
        } catch (Exception e) {
            log.error("list:latest 크기 제한 실패 - error: {}", e.getMessage(), e);
        }
    }
    
    // 사용자 참여 목록 크기 제한
    @Override
    public void limitUserJoinedList(Long userId) {
        try {
            String userJoinedKey = RedisKeyGenerator.generateUserRoomsKey(userId);
            Long count = redisTemplate.opsForZSet().zCard(userJoinedKey);
            
            if (count != null && count > MAX_USER_JOINED_SIZE) {
                // 오래된 것부터 제거
                long removeCount = count - MAX_USER_JOINED_SIZE;
                redisTemplate.opsForZSet().removeRange(userJoinedKey, 0, removeCount - 1);
                
                log.debug("user:{}:joined 크기 제한 - 제거: {}개, 남은: {}개", 
                        userId, removeCount, MAX_USER_JOINED_SIZE);
            }
        } catch (Exception e) {
            log.error("user:joined 크기 제한 실패 - user:{}, error: {}", 
                    userId, e.getMessage(), e);
        }
    }
    
    // 캐시 워밍
    @Override
    public void warmUpCache(List<DiscussionRoom> rooms, List<Integer> currentUsersCounts) {
        if (rooms == null || rooms.isEmpty()) {
            return;
        }
        
        if (currentUsersCounts == null || rooms.size() != currentUsersCounts.size()) {
            log.error("캐시 워밍 실패 - rooms와 currentUsersCounts 크기 불일치");
            return;
        }
        
        try {
            int cachedCount = 0;
            
            for (int i = 0; i < rooms.size(); i++) {
                DiscussionRoom room = rooms.get(i);
                int currentUsers = currentUsersCounts.get(i);
                
                DiscussionRoomCacheModel cached = DiscussionRoomCacheModel.fromDomainModel(room, currentUsers);
                String roomKey = RedisKeyGenerator.generateRoomInfoKey(room.getId());
                
                redisTemplate.opsForHash().putAll(roomKey, cached.toRedisHash());
                redisTemplate.expire(roomKey, TTL_ROOM_INFO);
                
                cachedCount++;
            }
            
            log.info("캐시 워밍 완료 - 총 {}개 방 캐싱", cachedCount);
            
        } catch (Exception e) {
            log.error("캐시 워밍 실패 - error: {}", e.getMessage(), e);
        }
    }
}
