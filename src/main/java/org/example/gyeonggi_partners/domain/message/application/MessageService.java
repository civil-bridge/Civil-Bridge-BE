package org.example.gyeonggi_partners.domain.message.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gyeonggi_partners.common.exception.BusinessException;
import org.example.gyeonggi_partners.common.exception.MessageException;
import org.example.gyeonggi_partners.domain.discussionRoom.exception.DiscussionRoomErrorCode;
import org.example.gyeonggi_partners.domain.discussionRoom.infra.persistence.discussionRoom.DiscussionRoomEntity;
import org.example.gyeonggi_partners.domain.discussionRoom.infra.persistence.discussionRoom.DiscussionRoomJpaRepository;
import org.example.gyeonggi_partners.domain.discussionRoom.infra.persistence.member.MemberJpaRepository;
import org.example.gyeonggi_partners.domain.message.api.MessageType;
import org.example.gyeonggi_partners.domain.message.api.dto.MessagePageResponse;
import org.example.gyeonggi_partners.domain.message.api.dto.MessageRequest;
import org.example.gyeonggi_partners.domain.message.api.dto.MessageResponse;
import org.example.gyeonggi_partners.domain.message.exception.MessageErrorCode;
import org.example.gyeonggi_partners.domain.message.infra.MessageEntity;
import org.example.gyeonggi_partners.domain.message.infra.MessageRepository;
import org.example.gyeonggi_partners.domain.user.exception.UserErrorCode;
import org.example.gyeonggi_partners.domain.user.infra.persistence.UserEntity;
import org.example.gyeonggi_partners.domain.user.infra.persistence.UserJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final RedisPublisher redisPublisher;
    private final MemberJpaRepository memberJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final DiscussionRoomJpaRepository discussionRoomJpaRepository;
    private final MessageRepository messageRepository;


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

    public void processJoinMessage(MessageRequest request, SimpMessageHeaderAccessor headerAccessor) {

        // 웹소켓 세션에 사용자 id 저장
        headerAccessor.getSessionAttributes().put("userId",request.getUserId());

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

}
