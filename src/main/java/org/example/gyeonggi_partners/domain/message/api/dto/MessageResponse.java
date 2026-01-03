package org.example.gyeonggi_partners.domain.message.api.dto;

import lombok.Builder;
import lombok.Getter;
import org.example.gyeonggi_partners.domain.message.infra.MessageEntity;

import java.time.LocalDateTime;

@Getter
@Builder
public class MessageResponse {
    private Long id; // 커서 페이징용 id
    private String content;
    private String userName;
    private LocalDateTime createdAt;

    public static MessageResponse of(MessageEntity messageEntity) {
        return MessageResponse.builder()
                .id(messageEntity.getId())
                .content(messageEntity.getContent())
                .userName(messageEntity.getUser().getName())
                .createdAt(messageEntity.getCreatedAt())
                .build();
    }
}