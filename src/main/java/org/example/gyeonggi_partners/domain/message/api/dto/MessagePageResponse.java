package org.example.gyeonggi_partners.domain.message.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
// 클라이언트가 다음 페이지가 있는지 다음 요청에 어떤 커서를 보낼지 알게 하기 위함
public class MessagePageResponse {

    private List<MessageResponse> messages;
    private Long nextCursor; // 다음 페이지 조회를 위한 커서 (마지막 메세지 id)
    private boolean hasNext; // 다음 페이지 존재 여부

    public static MessagePageResponse of(List<MessageResponse> messages, int requestSize) {

        boolean hasNext =messages.size() > requestSize;

        // 요청한 사이즈보다 더 조회시, 마지막 제거
        List<MessageResponse> content= hasNext ? messages.subList(0, requestSize) : messages;

        Long nextCursor = hasNext && !content.isEmpty() ? content.get(content.size()-1).getId() : null;

        return MessagePageResponse.builder()
                .messages(content)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

}
