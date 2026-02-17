package org.example.civilbridge.domain.discussionRoom.infra.persistence.discussionRoom;


import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.civilbridge.domain.common.BaseEntity;
import org.example.civilbridge.domain.discussionRoom.domain.model.AccessLevel;
import org.example.civilbridge.domain.discussionRoom.domain.model.DiscussionRoom;

/**
 * DiscussionRoom JPA 엔티티
 * DB와 매핑되는 영속성 객체
 */
@Entity
@Table(name = "discussion_rooms")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class DiscussionRoomEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "city", nullable = false, length = 50)
    private String city;

    @Column(name = "district", nullable = false, length = 50)
    private String district;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false)
    private AccessLevel accessLevel;

    @Version
    @Column(name = "version")
    private Long version;


    @Builder
    private DiscussionRoomEntity(Long id, String title, String description,
                                 String city, String district, AccessLevel accessLevel) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.city = city;
        this.district = district;
        this.accessLevel = accessLevel;
    }

    /**
     * 도메인 모델을 엔티티로 변환 (Domain -> Entity)
     */
    public static DiscussionRoomEntity fromDomain(DiscussionRoom discussionRoom) {
        return DiscussionRoomEntity.builder()
                .id(discussionRoom.getId())
                .title(discussionRoom.getTitle())
                .description(discussionRoom.getDescription())
                .city(discussionRoom.getCity())
                .district(discussionRoom.getDistrict())
                .accessLevel(discussionRoom.getAccessLevel())
                .build();
    }

    /**
     * 엔티티를 도메인 모델로 변환 (Entity -> Domain)
     */
    public DiscussionRoom toDomain() {
        return DiscussionRoom.restore(
                this.id,
                this.title,
                this.description,
                this.city,
                this.district,
                this.accessLevel,
                this.getCreatedAt(),
                this.getUpdatedAt(),
                this.getDeletedAt()
        );
    }
}
