-- V1__init.sql
-- Initial database schema setup (MySQL)


-- 사용자 테이블 (users)
-- Stores user account information.
CREATE TABLE users
(
    user_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    login_id       VARCHAR(30) UNIQUE NOT NULL,
    login_pw       VARCHAR(255)       NOT NULL COMMENT '비밀번호는 해시 처리하여 저장',
    name           VARCHAR(50)        NOT NULL,
    nickname       VARCHAR(50) UNIQUE NOT NULL,
    email          VARCHAR(50) UNIQUE NOT NULL,
    phone_number   VARCHAR(20) UNIQUE NOT NULL,
    role           VARCHAR(20)        NOT NULL DEFAULT 'USER',
    created_at     DATETIME           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME,
    deleted_at     DATETIME
) COMMENT '사용자 정보를 저장하는 테이블';


-- 논의방 테이블 (discussion_rooms)
-- Represents a chat room or a space for discussion.
CREATE TABLE discussion_rooms
(
    room_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(100)      NOT NULL,
    description     VARCHAR(255),
    city            VARCHAR(30)       NOT NULL,
    district        VARCHAR(30)       NOT NULL,
    version         BIGINT            NOT NULL DEFAULT 0,
    created_at      DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME,
    deleted_at      DATETIME
) COMMENT '논의가 이루어지는 공간(채팅방) 테이블';


-- 멤버 테이블 (members)
-- Junction table linking users and discussion_rooms (Many-to-Many).
CREATE TABLE members
(
    member_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    room_id         BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_members_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_members_discussion_room FOREIGN KEY (room_id) REFERENCES discussion_rooms (room_id) ON DELETE CASCADE,
    UNIQUE (user_id, room_id) -- 한 사용자는 한 논의방에 한 번만 참여 가능
) COMMENT '사용자와 논의방의 다대다 관계를 위한 조인 테이블';


-- 채팅 테이블 (chats)
-- Stores all messages sent within discussion rooms.
CREATE TABLE chat
(
    chat_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id         BIGINT            NOT NULL,
    sender_id       BIGINT,
    content         TEXT              NOT NULL,
    chat_type       ENUM('TEXT', 'ENTRY', 'EXIT') NOT NULL DEFAULT 'TEXT',
    created_at      DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_chat_discussion_room FOREIGN KEY (room_id) REFERENCES discussion_rooms (room_id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_sender FOREIGN KEY (sender_id) REFERENCES users (user_id) ON DELETE SET NULL -- 발신자 탈퇴 시 메시지는 남도록 SET NULL 처리
) COMMENT '논의방에서 주고받은 메시지 정보를 저장하는 테이블';


-- 솔루션 제안서 테이블 (proposals)
-- Stores proposals submitted within a discussion room.
CREATE TABLE proposals
(
    proposal_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id           BIGINT            NOT NULL,
    author_id         BIGINT,                      -- 마지막 저장자 ID (NULL 허용)
    title             VARCHAR(100)      NOT NULL,
    contents          JSON              NOT NULL,
    required_consents INTEGER           NOT NULL DEFAULT 1,
    consent_deadline  DATETIME,
    status            ENUM('DRAFTING', 'SAVING', 'PENDING_CONSENT', 'CONSENT_FAILED', 'READY_TO_SUBMIT', 'SUBMITTED') NOT NULL DEFAULT 'DRAFTING',
    consents          JSON,
    version           BIGINT            NOT NULL DEFAULT 1,
    created_at        DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME,
    deleted_at        DATETIME,

    -- 외래 키 제약조건
    CONSTRAINT fk_proposals_discussion_room FOREIGN KEY (room_id) REFERENCES discussion_rooms (room_id) ON DELETE CASCADE,
    CONSTRAINT fk_proposals_author FOREIGN KEY (author_id) REFERENCES users (user_id) ON DELETE SET NULL
) COMMENT '논의방 내에서 제출된 제안서를 관리하는 테이블';
