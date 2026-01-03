package org.example.gyeonggi_partners.domain.message.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.example.gyeonggi_partners.domain.message.api.MessageType;

@Getter
public class MessageRequest {
    private MessageType type;
    @NotBlank (message = "메세지 내용을 입력해주세요")
    private String content;
    private Long roomId;
    private Long userId;
}
