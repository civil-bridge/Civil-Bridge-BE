package org.example.civilbridge.domain.discussionRoom.infra.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.civilbridge.domain.discussionRoom.domain.model.DiscussionRoom;
import org.example.civilbridge.domain.discussionRoom.domain.repository.DiscussionRoomRepository;
import org.example.civilbridge.domain.discussionRoom.domain.repository.MemberRepository;
import org.example.civilbridge.domain.discussionRoom.infra.cache.dto.DiscussionRoomCacheModel;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

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

    private static final Duration TTL_ROOM_INFO = Duration.ofHours(24);
    private static final Duration TTL_RECENT_ROOMS = Duration.ofHours(1);
    private static final Duration TTL_USER_ROOM = Duration.ofHours(12);
    private static final Duration TTL_ROOM_MEMBERS = Duration.ofHours(24);

    private static final int MAX_LATEST_LIST_SIZE = 10_000;
    private static final int MAX_USER_JOINED_SIZE = 100;

    @Override
    public void saveNewRoomToRedis(DiscussionRoomCacheModel cachedRoom, Long creatorId, long timestamp) {
        try {
            String roomInfoKey = RedisKeyGenerator.generateRoomInfoKey(cachedRoom.getId());
            String recentRoomsKey = RedisKeyGenerator.generateRecentRoomsKey();
            String userRoomsKey = RedisKeyGenerator.generateUserRoomsKey(creatorId);
            String roomMembersKey = RedisKeyGenerator.generateRoomMembersKey(cachedRoom.getId());

            redisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                @SuppressWarnings("unchecked")
                public List<Object> execute(RedisOperations operations) {
                    operations.multi();

                    operations.opsForHash().putAll(roomInfoKey, cachedRoom.toRedisHash());
                    operations.expire(roomInfoKey, TTL_ROOM_INFO);

                    operations.opsForZSet().add(recentRoomsKey, cachedRoom.getId(), (double) timestamp);
                    operations.expire(recentRoomsKey, TTL_RECENT_ROOMS);

                    operations.opsForZSet().add(userRoomsKey, cachedRoom.getId(), (double) timestamp);
                    operations.expire(userRoomsKey, TTL_USER_ROOM);

                    operations.opsForList().rightPush(roomMembersKey, creatorId);
                    operations.expire(roomMembersKey, TTL_ROOM_MEMBERS);

                    return operations.exec();
                }
            });

            limitLatestSize();
            limitUserJoinedList(creatorId);

            log.debug("캐시 저장 성공 - room:{}, creator:{}", cachedRoom.getId(), creatorId);

        } catch (Exception e) {
            log.error("캐시 저장 실패 - room:{}, error: {}", cachedRoom.getId(), e.getMessage(), e);
        }
    }

    @Override
    public void addUserToRoom(Long userId, Long roomId, long timestamp) {
        try {
            String userJoinedKey = RedisKeyGenerator.generateUserRoomsKey(userId);
            String roomMembersKey = RedisKeyGenerator.generateRoomMembersKey(roomId);
            String roomKey = RedisKeyGenerator.generateRoomInfoKey(roomId);

            redisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                @SuppressWarnings("unchecked")
                public List<Object> execute(RedisOperations operations) {
                    operations.multi();

                    operations.opsForZSet().add(userJoinedKey, roomId, (double) timestamp);
                    operations.expire(userJoinedKey, TTL_USER_ROOM);

                    operations.opsForList().rightPush(roomMembersKey, userId);
                    operations.expire(roomMembersKey, TTL_ROOM_MEMBERS);

                    operations.opsForHash().increment(roomKey, "currentUsers", 1);

                    return operations.exec();
                }
            });

            limitUserJoinedList(userId);

            log.debug("논의방 입장 - user:{}, room:{}", userId, roomId);

        } catch (Exception e) {
            log.error("논의방 입장 실패 - user:{}, room:{}, error: {}", userId, roomId, e.getMessage(), e);
        }
    }

    @Override
    public void removeUserFromRoom(Long userId, Long roomId) {
        try {
            String userJoinedKey = RedisKeyGenerator.generateUserRoomsKey(userId);
            String roomMembersKey = RedisKeyGenerator.generateRoomMembersKey(roomId);
            String roomKey = RedisKeyGenerator.generateRoomInfoKey(roomId);

            redisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                @SuppressWarnings("unchecked")
                public List<Object> execute(RedisOperations operations) {
                    operations.multi();

                    operations.opsForZSet().remove(userJoinedKey, roomId);
                    operations.opsForList().remove(roomMembersKey, 0, userId);
                    operations.opsForHash().increment(roomKey, "currentUsers", -1);

                    return operations.exec();
                }
            });

            log.debug("논의방 퇴장 - user:{}, room:{}", userId, roomId);

        } catch (Exception e) {
            log.error("논의방 퇴장 실패 - user:{}, room:{}, error: {}", userId, roomId, e.getMessage(), e);
        }
    }

    @Override
    public void evictRoomCache(Long roomId, Long creatorId) {
        try {
            String roomKey = RedisKeyGenerator.generateRoomInfoKey(roomId);
            String roomMembersKey = RedisKeyGenerator.generateRoomMembersKey(roomId);
            String latestKey = RedisKeyGenerator.generateRecentRoomsKey();
            String userJoinedKey = RedisKeyGenerator.generateUserRoomsKey(creatorId);

            redisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                @SuppressWarnings("unchecked")
                public List<Object> execute(RedisOperations operations) {
                    operations.multi();

                    operations.delete(roomKey);
                    operations.delete(roomMembersKey);
                    operations.opsForZSet().remove(latestKey, roomId);
                    operations.opsForZSet().remove(userJoinedKey, roomId);

                    return operations.exec();
                }
            });

            log.info("캐시 삭제 완료 - room:{}, creator:{}", roomId, creatorId);

        } catch (Exception e) {
            log.error("캐시 삭제 실패 - room:{}, creator:{}, error: {}", roomId, creatorId, e.getMessage(), e);
        }
    }

    @Override
    public Optional<DiscussionRoomCacheModel> retrieveCachingRoom(Long roomId) {
        try {
            String roomKey = RedisKeyGenerator.generateRoomInfoKey(roomId);
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(roomKey);

            if (entries.isEmpty()) {
                log.debug("캐시 미스 - DB 조회 시작 - room:{}", roomId);

                Optional<DiscussionRoom> roomOpt = discussionRoomRepository.findById(roomId);
                if (roomOpt.isEmpty()) {
                    log.debug("DB에도 존재하지 않음 - room:{}", roomId);
                    return Optional.empty();
                }

                int currentUsers = memberRepository.countByRoomId(roomId);
                DiscussionRoomCacheModel model = DiscussionRoomCacheModel.fromDomainModel(roomOpt.get(), currentUsers);

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

    @Override
    public Map<Long, Optional<DiscussionRoomCacheModel>> getCachedRoomsAll(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<String> keys = roomIds.stream()
                    .map(RedisKeyGenerator::generateRoomInfoKey)
                    .collect(Collectors.toList());

            List<Object> results = redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                @SuppressWarnings("unchecked")
                public Object execute(RedisOperations operations) {
                    for (String key : keys) {
                        operations.opsForHash().entries(key);
                    }
                    return null;
                }
            });

            Map<Long, Optional<DiscussionRoomCacheModel>> resultMap = new LinkedHashMap<>();
            for (int i = 0; i < roomIds.size(); i++) {
                Long roomId = roomIds.get(i);
                @SuppressWarnings("unchecked")
                Map<Object, Object> entries = (Map<Object, Object>) results.get(i);
                if (entries == null || entries.isEmpty()) {
                    log.debug("캐시 미스 (pipeline) - room:{}", roomId);
                    resultMap.put(roomId, Optional.empty());
                } else {
                    resultMap.put(roomId, Optional.ofNullable(DiscussionRoomCacheModel.fromRedisHash(entries)));
                    log.debug("캐시 히트 (pipeline) - room:{}", roomId);
                }
            }
            return resultMap;

        } catch (Exception e) {
            log.error("Pipeline 캐시 조회 실패 - rooms:{}, error: {}", roomIds, e.getMessage(), e);
            Map<Long, Optional<DiscussionRoomCacheModel>> fallback = new LinkedHashMap<>();
            roomIds.forEach(id -> fallback.put(id, Optional.empty()));
            return fallback;
        }
    }

    @Override
    public void cacheRoomInfo(DiscussionRoomCacheModel model) {
        try {
            String roomKey = RedisKeyGenerator.generateRoomInfoKey(model.getId());
            redisTemplate.opsForHash().putAll(roomKey, model.toRedisHash());
            redisTemplate.expire(roomKey, TTL_ROOM_INFO);
            log.debug("논의방 정보 캐싱 완료 - room:{}", model.getId());
        } catch (Exception e) {
            log.error("논의방 정보 캐싱 실패 - room:{}, error: {}", model.getId(), e.getMessage(), e);
        }
    }

    @Override
    public List<Long> retrieveRoomMembers(Long roomId) {
        try {
            String roomMembersKey = RedisKeyGenerator.generateRoomMembersKey(roomId);
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

    @Override
    public void cacheRoomMembers(Long roomId, List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }
        try {
            String roomMembersKey = RedisKeyGenerator.generateRoomMembersKey(roomId);
            redisTemplate.delete(roomMembersKey);
            redisTemplate.opsForList().rightPushAll(roomMembersKey, memberIds.toArray());
            redisTemplate.expire(roomMembersKey, TTL_ROOM_MEMBERS);
            log.debug("멤버 목록 캐싱 - room:{}, count:{}", roomId, memberIds.size());
        } catch (Exception e) {
            log.error("멤버 목록 캐싱 실패 - room:{}, error: {}", roomId, e.getMessage(), e);
        }
    }

    private void limitLatestSize() {
        try {
            String latestKey = RedisKeyGenerator.generateRecentRoomsKey();
            Long count = redisTemplate.opsForZSet().zCard(latestKey);

            if (count != null && count > MAX_LATEST_LIST_SIZE) {
                long removeCount = count - MAX_LATEST_LIST_SIZE;
                redisTemplate.opsForZSet().removeRange(latestKey, 0, removeCount - 1);
                log.debug("list:latest 크기 제한 - 제거: {}개, 남은: {}개", removeCount, MAX_LATEST_LIST_SIZE);
            }
        } catch (Exception e) {
            log.error("list:latest 크기 제한 실패 - error: {}", e.getMessage(), e);
        }
    }

    private void limitUserJoinedList(Long userId) {
        try {
            String userJoinedKey = RedisKeyGenerator.generateUserRoomsKey(userId);
            Long count = redisTemplate.opsForZSet().zCard(userJoinedKey);

            if (count != null && count > MAX_USER_JOINED_SIZE) {
                long removeCount = count - MAX_USER_JOINED_SIZE;
                redisTemplate.opsForZSet().removeRange(userJoinedKey, 0, removeCount - 1);
                log.debug("user:{}:joined 크기 제한 - 제거: {}개, 남은: {}개", userId, removeCount, MAX_USER_JOINED_SIZE);
            }
        } catch (Exception e) {
            log.error("user:joined 크기 제한 실패 - user:{}, error: {}", userId, e.getMessage(), e);
        }
    }
}
