package org.example.gyeonggi_partners.domain.discussionRoom.infra.persistence.discussionRoom;

import lombok.RequiredArgsConstructor;
import org.example.gyeonggi_partners.domain.discussionRoom.domain.model.DiscussionRoom;
import org.example.gyeonggi_partners.domain.discussionRoom.domain.repository.DiscussionRoomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * DiscussionRoomRepository 구현체
 * 도메인 인터페이스를 JPA로 구현
 */
@Repository
@RequiredArgsConstructor
public class DiscussionRoomRepositoryImpl implements DiscussionRoomRepository {

    private final DiscussionRoomJpaRepository discussionRoomJpaRepository;

    @Override
    public DiscussionRoom save(DiscussionRoom discussionRoom) {
        DiscussionRoomEntity entity = DiscussionRoomEntity.fromDomain(discussionRoom);
        DiscussionRoomEntity savedEntity = discussionRoomJpaRepository.save(entity);
        return savedEntity.toDomain();
    }

    //findBy도 Optinal로 바꿔야할듯?
    @Override
    public Optional<DiscussionRoom> findById(Long id) {
        return discussionRoomJpaRepository.findById(id)
                .map(DiscussionRoomEntity::toDomain);
    }

    @Override
    public void softDelete(Long roomId) {
        discussionRoomJpaRepository.softDelete(roomId);
    }

    @Override
    public Page<DiscussionRoom> findAllByOrderByCreatedAtDesc(Pageable pageable) {
        Page<DiscussionRoomEntity> entityPage = discussionRoomJpaRepository
                .findAllByDeletedAtIsNullOrderByCreatedAtDesc(pageable);
        return entityPage.map(DiscussionRoomEntity::toDomain);
    }

    @Override
    public List<DiscussionRoom> findAllByIdIn(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return discussionRoomJpaRepository.findAllByIdIn(ids).stream()
                .map(DiscussionRoomEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<DiscussionRoom> findByIdWithLock(Long id) {
        return discussionRoomJpaRepository.findByIdWithLock(id)
                .map(DiscussionRoomEntity::toDomain);
    }
}
