package org.example.gyeonggi_partners.domain.message.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gyeonggi_partners.domain.message.api.dto.MessageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher {

    // MessageController로부터 request를 받아 설정된 redis 채널로 발행
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic topic;

    public void publish(MessageRequest request) {
        
        log.info("[RedisPublisher] 메시지 발행 시작 - channel: {}, roomId: {}, userId: {}, type: {}", 
                topic.getTopic(), request.getRoomId(), request.getUserId(), request.getType());

        //ChannelTopic으로 설정된 채널에 메세지를 발행
        // convertAndSend는 주어진 객체를 직렬화기로 변환 후 지정한 토픽에 발행
        redisTemplate.convertAndSend(topic.getTopic(), request);
        
        log.info("[RedisPublisher] 메시지 발행 완료 - channel: {}", topic.getTopic());
    }


}
