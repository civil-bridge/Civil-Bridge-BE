package org.example.civilbridge.domain.discussionRoom.infra.persistence.member;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.civilbridge.domain.discussionRoom.domain.model.Member;

import java.time.LocalDateTime;

/**
 * Member JPA 엔티티
 * DB의 members 테이블과 매핑되는 영속성 객체
 */
@Entity
@Table(name = "members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "room_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private MemberEntity(Long id, Long userId, Long roomId, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.roomId = roomId;
        this.createdAt = createdAt;
    }

    /**
     * 도메인 모델을 엔티티로 변환 (Domain -> Entity)
     */
    public static MemberEntity fromDomain(Member member) {
        return MemberEntity.builder()
                .id(member.getId())
                .userId(member.getUserId())
                .roomId(member.getRoomId())
                .createdAt(member.getCreatedAt())
                .build();
    }

    /**
     * 엔티티를 도메인 모델로 변환 (Entity -> Domain)
     */
    public Member toDomain() {
        return Member.restore(
                this.id,
                this.userId,
                this.roomId,
                this.createdAt
        );
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
