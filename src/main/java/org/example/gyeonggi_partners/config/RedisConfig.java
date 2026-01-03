package org.example.gyeonggi_partners.config;

import org.example.gyeonggi_partners.domain.message.application.RedisSubscriber;
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
     * RedisTemplate 빈 설정
     * 
     * <p>이 설정이 없으면 기본 Java 직렬화를 사용하여 데이터가 이진 형태로 저장됩니다.
     * JSON 직렬화를 설정하여 Redis CLI에서 데이터 확인 및 디버깅이 용이하도록 합니다.</p>
     * 
     * <ul>
     *   <li>Key: String 형식 (예: "user:1")</li>
     *   <li>Value: JSON 형식 (예: {"id":1,"name":"홍길동"})</li>
     * </ul>
     *
     * @param connectionFactory Redis 연결 팩토리
     * @return JSON 직렬화가 설정된 RedisTemplate
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
        // Hash Value도 JSON으로 직렬화
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    // Redis의 sub/pub 메세지 처리를 담당하는 리스너 컨테이너
    // 지정 채널을 비동기적으로 리스닝하고, 메세지 도착시 등록된 리스너에게 전달, 스레드 관리와 메시지 디스패칭 자동 처리
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

    @Bean
    // 실제 메세지를 처리할 subbscriber를 위한 어댑터
    // subscriber 클래스와 해당 클래스의 handleMessage 메서드를 지정하여 호출
    public MessageListenerAdapter messageListenerAdapter(
            RedisSubscriber subcriber,
            RedisTemplate<String, Object> redisTemplate
    ) {

        MessageListenerAdapter adapter = new MessageListenerAdapter(subcriber,"handleMessage");
        adapter.setSerializer(redisTemplate.getStringSerializer()); // redisConfig에서 역직렬화하여 RedisSubscriber와 책임 분리
        return adapter;
    }

    // sub/pub 채널을 정의, 모든 채팅 메세지는 해당 채널을 통해 발행/구독됨
    @Bean
    public ChannelTopic channelTopic() {
        return new ChannelTopic("chatChannel");
    }

}
