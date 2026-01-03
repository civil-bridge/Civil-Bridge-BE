package org.example.gyeonggi_partners.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        // 클라이언트가 서버와 웹소켓 연결을 시작할 엔드포인트 등록
        // /gyeonggi_partners-chat 경로로 sockjs 연결 허용
        // sockjs는 웹소켓을 지원하지 않는 브라우저에도 폴백을 제공하기 위한 기능
        registry.addEndpoint("/gyeonggi_partners-chat").withSockJS();

    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // 어플리케이션 목적지 접두사 설정
        // 클라이언트가 /chat으로 메세지를 보내면 @MessageMapping 어노테이션이 붙은 컨트롤러 메서드가 이를 담당하여 처리
        registry.setApplicationDestinationPrefixes("/app");

        // 메세지 브로커가 처리할 목적집 접두사 설정
        // /topic으로 시작하는 목적지를 구독하는 클라이언트에게 메세지 브로드캐스팅
        // 빠른 개발을 위해 simpleBroker 사용, 메세지를 단일 서버로밖에 보내지 못 하므로 redis 도입
        registry.enableSimpleBroker("/topic");

    }

}
