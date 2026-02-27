package org.example.civilbridge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


/**
 * WebSocket 연결 엔드포인트 등록 및 메시지 라우팅 규칙 설정
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     *클라이언트가 서버와 웹소켓 연결을 시작할 엔드포인트 등록
     * */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/gyeonggi_partners-chat")
                .setAllowedOrigins("http://localhost:5173")
                .withSockJS();

    }


    /**
     * STOMP 메시지 라우팅 규칙 정의
     * 이 설정이 없으면 클라이언트가 메시지를 보내도 어디로 전달할지 몰라 라우팅 실패
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic");

    }

}
