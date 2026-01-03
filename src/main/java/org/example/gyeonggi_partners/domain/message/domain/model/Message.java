package org.example.gyeonggi_partners.domain.message.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Message{

    private Long id;
    private Long userId; // 도메인에서 jpa 엔티티를 참조하는 건 괜찮으나, 그 반대는 지양해야 함
    private Long roomId; // 따라서 도메인인 객체Id로 변경
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

}
