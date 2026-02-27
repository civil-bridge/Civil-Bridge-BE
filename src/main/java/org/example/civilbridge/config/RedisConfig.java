package org.example.civilbridge.config;

import org.example.civilbridge.domain.message.application.RedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정 클래스
 */
@Configuration
public class RedisConfig {

    /**
     * Redis 데이터 직렬화 설정
     * 이 설정이 없으면 데이터가 이진 형태로 저장되어 Redis CLI에서 확인/디버깅 불가
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key는 String으로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        // Value는 JSON으로 직렬화
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        // Hash Key도 String으로 직렬화
        template.setHashKeySerializer(new StringRedisSerializer());
        // Hash Value도 String으로 직렬화 (toRedisHash()에서 이미 String 변환 완료, HINCRBY 호환)
        template.setHashValueSerializer(new StringRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 채팅 메시지가 오가는 Redis 채널 정의
     * 이 설정이 없으면 메시지를 어느 채널로 발행/구독할지 몰라 실시간 채팅 불가
     */
    @Bean
    public ChannelTopic channelTopic() {
        return new ChannelTopic("chatChannel");
    }

    /**
     * Redis 메시지 수신 시 처리할 메서드 연결
     * 이 설정이 없으면 메시지가 도착해도 누가 처리할지 몰라 수신 불가
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter,
            ChannelTopic channelTopic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // channelTopic에 대해서 listenserAdapter가 메세지를 수신하도록 설정
        container.addMessageListener(listenerAdapter, channelTopic);
        return container;
    }

    /**
     * Redis 채널 실시간 리스닝 설정
     * 이 설정이 없으면 채널을 아무도 감시하지 않아 메시지 수신 자체가 불가
     */
    @Bean
    public MessageListenerAdapter messageListenerAdapter(
            RedisSubscriber subcriber,
            RedisTemplate<String, Object> redisTemplate
    ) {

        MessageListenerAdapter adapter = new MessageListenerAdapter(subcriber,"handleMessage");
        adapter.setSerializer(redisTemplate.getStringSerializer()); // redisConfig에서 역직렬화하여 RedisSubscriber와 책임 분리
        return adapter;
    }


}
