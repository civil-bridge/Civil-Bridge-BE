package org.example.gyeonggi_partners.domain.message.infra;

import jakarta.persistence.*;
import lombok.*;
import org.example.gyeonggi_partners.domain.common.BaseEntity;
import org.example.gyeonggi_partners.domain.discussionRoom.infra.persistence.discussionRoom.DiscussionRoomEntity;
import org.example.gyeonggi_partners.domain.user.infra.persistence.UserEntity;

@Entity
@Getter
@Table(name = "messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class MessageEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="room_id", nullable=false)
    private DiscussionRoomEntity discussionRoom;


    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

}
