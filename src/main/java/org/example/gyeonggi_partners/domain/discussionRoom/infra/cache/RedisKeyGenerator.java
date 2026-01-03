package org.example.gyeonggi_partners.domain.discussionRoom.infra.cache;

/**
 * Redis Key 생성 유틸리티 클래스
 * 
 * <p>결정사항 3-1: 단순 패턴 사용 (room:{id}, list:latest, user:{id}:joined)</p>
 * 
 * <p>모든 Redis Key는 이 클래스를 통해 생성하여 일관성 보장</p>
 */
public class RedisKeyGenerator {
    
    // Key 패턴 상수
    private static final String ROOM_INFO_KEY = "room:%d";                    // room:{id}
    private static final String ROOM_MEMBERS_KEY = "room:%d:members";           // room:{id}:members
    private static final String RECENT_ROOMS_KEY = "list:latest";                // list:latest
    private static final String USER_ROOMS_KEY = "user:%d:joined";             // user:{id}:joined
    
    /**
     * 논의방 상세 정보 Key 생성
     * 
     * @param roomId 논의방 ID
     * @return room:{roomId}
     */
    public static String generateRoomInfoKey(Long roomId) {
        return String.format(ROOM_INFO_KEY, roomId);
    }
    
    /**
     * 논의방 멤버 목록 Key 생성
     * 
     * @param roomId 논의방 ID
     * @return room:{roomId}:members
     */
    public static String generateRoomMembersKey(Long roomId) {
        return String.format(ROOM_MEMBERS_KEY, roomId);
    }
    
    /**
     * 전체 최신 논의방 목록 Key
     * 
     * @return list:latest
     */
    public static String generateRecentRoomsKey() {
        return RECENT_ROOMS_KEY;
    }
    
    /**
     * 사용자가 참여한 논의방 목록 Key 생성
     * 
     * @param userId 사용자 ID
     * @return user:{userId}:joined
     */
    public static String generateUserRoomsKey(Long userId) {
        return String.format(USER_ROOMS_KEY, userId);
    }
}
