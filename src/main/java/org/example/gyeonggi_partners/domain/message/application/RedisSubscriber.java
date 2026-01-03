package org.example.gyeonggi_partners.domain.message.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gyeonggi_partners.domain.message.api.dto.MessageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // redis에서 메세지를 발행하면, Listener가 해당 메세지를 webSocket 클라이언트로 브로드캐스팅함
    public void handleMessage(String message) {
        try {
            log.debug("[RedisSubscriber] 메시지 수신: {}", message);
            
            // JSON String을 MessageRequest 객체로 변환
            MessageRequest request = objectMapper.readValue(message, MessageRequest.class);
            
            log.info("[RedisSubscriber] 브로드캐스팅 시작 - roomId: {}, userId: {}, type: {}", 
                    request.getRoomId(), request.getUserId(), request.getType());

            // 해당 채팅방을 구독하고 있는 클라이언트에게 메세지를 보냄
            // 목적지는 /topic/room/{roomId}
            messagingTemplate.convertAndSend("/topic/room/"+request.getRoomId(), request);
            
            log.info("[RedisSubscriber] 브로드캐스팅 완료 - /topic/room/{}", request.getRoomId());

        } catch (Exception e) {
            log.error("!!!메세지 처리 중 알 수 없는 오류 발생!!! 메세지 내용: {}", message, e);
        } // 예외를 던져버리면 redis 스레드가 죽어버려서 단순 로그만 남김
    }

}
