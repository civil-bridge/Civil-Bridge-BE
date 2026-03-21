package org.example.civilbridge.domain.message.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.civilbridge.common.exception.BusinessException;
import org.example.civilbridge.common.exception.MessageException;
import org.example.civilbridge.domain.discussionRoom.exception.DiscussionRoomErrorCode;
import org.example.civilbridge.domain.discussionRoom.infra.cache.RedisKeyGenerator;
import org.example.civilbridge.domain.discussionRoom.infra.persistence.discussionRoom.DiscussionRoomEntity;
import org.example.civilbridge.domain.discussionRoom.infra.persistence.discussionRoom.DiscussionRoomJpaRepository;
import org.example.civilbridge.domain.discussionRoom.infra.persistence.member.MemberJpaRepository;
import org.example.civilbridge.domain.message.api.MessageType;
import org.example.civilbridge.domain.message.api.dto.MessagePageResponse;
import org.example.civilbridge.domain.message.api.dto.MessageRequest;
import org.example.civilbridge.domain.message.api.dto.MessageResponse;
import org.example.civilbridge.domain.message.exception.MessageErrorCode;
import org.example.civilbridge.domain.message.infra.MessageEntity;
import org.example.civilbridge.domain.message.infra.MessageRepository;
import org.example.civilbridge.domain.user.exception.UserErrorCode;
import org.example.civilbridge.domain.user.infra.persistence.UserEntity;
import org.example.civilbridge.domain.user.infra.persistence.UserJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final RedisPublisher redisPublisher;
    private final MemberJpaRepository memberJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final DiscussionRoomJpaRepository discussionRoomJpaRepository;
    private final MessageRepository messageRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MessageBatchWriter messageBatchWriter;
    private final ObjectMapper objectMapper;


    @Transactional
    public void processChatMessage(MessageRequest request, SimpMessageHeaderAccessor headerAccessor) {

        Long sessionUserId= (Long) headerAccessor.getSessionAttributes().get("userId");

        if (sessionUserId == null || !sessionUserId.equals(request.getUserId())) {
            throw new BusinessException(MessageErrorCode.MESSAGE_USER_INCOINSISTENCY);
        }

        // 메세지 유효성 검증
        validateMessage(request);

        //멤버 권한 확인
        validateMembership(request.getUserId(),request.getRoomId());

        // 엔티티 조회
        UserEntity userEntity=userJpaRepository.findById(request.getUserId())
                .orElseThrow(()-> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        DiscussionRoomEntity roomEntity= discussionRoomJpaRepository.findById(request.getRoomId())
                .orElseThrow(()-> new BusinessException(DiscussionRoomErrorCode.ROOM_NOT_FOUND));

        // db에 메세지 저장
        MessageEntity messageEntity = MessageEntity.builder()
                .content(request.getContent())
                .user(userEntity)
                .discussionRoom(roomEntity)
                .build();

        messageRepository.save(messageEntity);

        // 메세지 타입이 chat일 경우 redis로 발행
        if (request.getType().equals(MessageType.CHAT)) {
            redisPublisher.publish(request);
        }
    }

    public void processChatMessageForHttp(MessageRequest request, Long userId) {
        validateMessage(request);
        validateMembershipFromCache(request.getUserId(), request.getRoomId());

        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "userId", request.getUserId(),
                    "roomId", request.getRoomId(),
                    "content", request.getContent()
            ));
            stringRedisTemplate.opsForList().leftPush(MessageBatchWriter.QUEUE_KEY, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Long queueSize = stringRedisTemplate.opsForList().size(MessageBatchWriter.QUEUE_KEY);
        if (queueSize != null && queueSize >= MessageBatchWriter.BATCH_SIZE) {
            messageBatchWriter.flush();
        }
    }

    public void processJoinMessage(MessageRequest request, SimpMessageHeaderAccessor headerAccessor) {
        // userId는 STOMP CONNECT 시점에 JWT로부터 StompChannelInterceptor가 세션에 저장

        //메세지 타입이 join인 경우, redis로 발행
        if (request.getType().equals(MessageType.JOIN)) {
            redisPublisher.publish(request);
        }
    }

    @Transactional(readOnly = true)
    public MessagePageResponse getMessages(Long roomId, Long cursor, int size ) {

        // 다음페이지 존재 여부를 판단하기 위해 size+1개 조회
        PageRequest pageRequest = PageRequest.of(0, size+1);

        List<MessageEntity> messages;

        if (cursor==null) {
            // 초기  메세지 처리, 페이징 x
            messages= messageRepository.findLatesetMessages(roomId, pageRequest);
        } else {
            messages=messageRepository.findMessagesBeforeCursor(roomId, cursor, pageRequest);
        }

        // <- Fetch Join된 User 엔티티에서 이름을 꺼냄
        List<MessageResponse> messageResponses = messages.stream()
                .map(MessageResponse::of)
                .toList();

        return MessagePageResponse.of(messageResponses,size);

    }


    private void validateMessage(MessageRequest request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new MessageException(MessageErrorCode.MESSAGE_CONTENT_EMPTY);
        }
        if (request.getContent().length() > 2000) {
            throw new MessageException(MessageErrorCode.MESSAGE_TOO_LONG);
        }
    }

    private void validateMembership(Long userId, Long roomId) {
        if (!memberJpaRepository.existsByUserIdAndRoomId(userId, roomId)) {
            throw new MessageException(DiscussionRoomErrorCode.NOT_A_ROOM_MEMBER);
        }
    }

    private void validateMembershipFromCache(Long userId, Long roomId) {
        String key = RedisKeyGenerator.generateUserRoomsKey(userId);
        Double score = redisTemplate.opsForZSet().score(key, roomId);
        if (score == null) {
            throw new MessageException(DiscussionRoomErrorCode.NOT_A_ROOM_MEMBER);
        }
    }

}
