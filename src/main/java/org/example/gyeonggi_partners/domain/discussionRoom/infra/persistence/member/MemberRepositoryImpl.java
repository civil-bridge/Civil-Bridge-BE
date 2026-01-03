package org.example.gyeonggi_partners.domain.discussionRoom.infra.persistence.member;

import lombok.RequiredArgsConstructor;
import org.example.gyeonggi_partners.domain.discussionRoom.domain.model.Member;
import org.example.gyeonggi_partners.domain.discussionRoom.domain.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MemberRepository 구현체
 * 도메인 인터페이스를 JPA로 구현
 */
@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {

    private final MemberJpaRepository memberJpaRepository;

    @Override
    public Member save(Member member) {
        MemberEntity entity = MemberEntity.fromDomain(member);
        MemberEntity savedEntity = memberJpaRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public boolean existsByUserIdAndRoomId(Long userId, Long roomId) {
        return memberJpaRepository.existsByUserIdAndRoomId(userId, roomId);
    }

    @Override
    public void deleteByUserIdAndRoomId(Long userId, Long roomId) {
        memberJpaRepository.deleteByUserIdAndRoomId(userId, roomId);
    }

    @Override
    public int countByRoomId(Long roomId) {
        return memberJpaRepository.countByRoomId(roomId);
    }

    @Override
    public Page<Long> findRoomIdsByUserId(Long userId, Pageable pageable) {
        return memberJpaRepository.findRoomIdsByUserIdOrderByJoinedAtDesc(userId, pageable);
    }

    @Override
    public Map<Long, Integer> countByRoomIds(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return new HashMap<>();
        }

        List<MemberJpaRepository.RoomMemberCount> counts = memberJpaRepository.countByRoomIds(roomIds);

        return counts.stream()
                .collect(Collectors.toMap(
                        MemberJpaRepository.RoomMemberCount::getRoomId,
                        count -> count.getCount().intValue()
                ));
    }
}
