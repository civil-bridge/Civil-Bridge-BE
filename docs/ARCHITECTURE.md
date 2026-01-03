# ARCHITECTURE.md

경기 파트너스의 소프트웨어 아키텍처를 상세히 설명합니다.

## 목차
1. [시스템 아키텍처 개요](#시스템-아키텍처-개요)
2. [레이어드 아키텍처](#레이어드-아키텍처)
3. [도메인 주도 설계 (DDD)](#도메인-주도-설계-ddd)
4. [헥사고날 아키텍처](#헥사고날-아키텍처)
5. [인프라 구조](#인프라-구조)
6. [보안 아키텍처](#보안-아키텍처)
7. [데이터 흐름](#데이터-흐름)
8. [주요 디자인 패턴](#주요-디자인-패턴)

---

## 시스템 아키텍처 개요

### 기술 스택
```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│          Spring MVC REST API + WebSocket (STOMP)        │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                   Application Layer                      │
│            Service Layer (Business Logic)                │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                     Domain Layer                         │
│          Domain Models + Repository Interfaces           │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                 Infrastructure Layer                     │
│    JPA Entities + Redis Cache + Email Notification      │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                   Persistence Layer                      │
│         PostgreSQL 15    +    Redis 7.4                 │
└─────────────────────────────────────────────────────────┘
```

### 핵심 아키텍처 원칙
- **도메인 주도 설계 (DDD)**: 비즈니스 도메인을 중심으로 모듈 구성
- **헥사고날 아키텍처**: 도메인 로직과 인프라 기술의 분리
- **의존성 역전 원칙 (DIP)**: 고수준 모듈이 저수준 모듈에 의존하지 않음
- **클린 아키텍처**: 계층 간 명확한 책임 분리

---

## 레이어드 아키텍처

### 4-Layer Architecture

각 도메인은 4개의 레이어로 구성됩니다:

```
src/main/java/org/example/gyeonggi_partners/
└── domain/
    └── {domain-name}/
        ├── api/                    # 🔵 Presentation Layer
        │   ├── {Domain}Controller.java
        │   └── dto/
        │       ├── *Request.java
        │       └── *Response.java
        │
        ├── application/            # 🟢 Application Layer
        │   └── {Domain}Service.java
        │
        ├── domain/                 # 🟡 Domain Layer (핵심)
        │   ├── model/
        │   │   └── {Domain}.java   # 순수 도메인 객체 (JPA 독립적)
        │   ├── repository/
        │   │   └── {Domain}Repository.java  # 인터페이스만 정의
        │   └── notifier/           # 도메인 추상화 (선택)
        │
        ├── infra/                  # 🔴 Infrastructure Layer
        │   ├── persistence/
        │   │   ├── {Domain}Entity.java       # JPA 엔티티
        │   │   ├── {Domain}JpaRepository.java
        │   │   └── {Domain}RepositoryImpl.java
        │   ├── cache/              # Redis 캐시 (discussionRoom)
        │   └── notification/       # 외부 서비스 연동 (user)
        │
        └── exception/              # 도메인 예외
            └── {Domain}ErrorCode.java
```

### 계층별 상세 설명

#### 1️⃣ API Layer (Presentation)
**역할**: HTTP 요청/응답 처리, DTO 변환

```java
@RestController
@RequestMapping("/api/discussion-rooms")
public class DiscussionRoomController {
    private final DiscussionRoomService service;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<JoinRoomRes>> createRoom(
        @RequestBody CreateDiscussionRoomReq request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        JoinRoomRes response = service.createRoom(request, userDetails.getUserId());
        return ResponseEntity.status(CREATED)
            .body(ApiResponse.success(response, "논의방이 생성되었습니다."));
    }
}
```

**특징**:
- `@RestController`: REST API 엔드포인트 노출
- DTO 사용: `*Request`, `*Response` (도메인 모델 직접 노출 방지)
- Spring Security 통합: `@AuthenticationPrincipal`로 인증된 사용자 정보 획득
- 표준화된 응답: `ApiResponse<T>` 래퍼 사용

#### 2️⃣ Application Layer (Service)
**역할**: 비즈니스 로직 오케스트레이션, 트랜잭션 관리

```java
@Service
@Transactional
public class DiscussionRoomService {
    private final DiscussionRoomRepository discussionRoomRepository;
    private final UserRepository userRepository;
    private final DiscussionRoomCacheRepository cacheRepository;

    public JoinRoomRes createRoom(CreateDiscussionRoomReq request, Long userId) {
        // 1. 도메인 모델 생성 (팩토리 메서드)
        DiscussionRoom room = DiscussionRoom.create(
            request.getTitle(),
            request.getDescription(),
            request.getRegion(),
            request.getAccessLevel()
        );

        // 2. 저장 (Repository 인터페이스 사용)
        DiscussionRoom savedRoom = discussionRoomRepository.save(room);

        // 3. 캐시 업데이트 (Write-Through)
        cacheRepository.saveNewRoomToRedis(savedRoom);

        // 4. 생성자 자동 입장
        Member creator = Member.create(userId, savedRoom.getId());
        memberRepository.save(creator);

        return JoinRoomRes.from(savedRoom, List.of(creator));
    }
}
```

**특징**:
- `@Transactional`: 트랜잭션 경계 설정
- 여러 Repository 조율
- 도메인 로직 호출 (직접 구현하지 않음)
- 인프라 서비스 조율 (캐시, 이메일 등)

#### 3️⃣ Domain Layer (핵심)
**역할**: 비즈니스 규칙, 도메인 로직 캡슐화

##### Domain Model (순수 자바 객체)
```java
@Getter
public class DiscussionRoom {
    private final Long id;
    private final String title;
    private final String description;
    private final Region region;
    private final AccessLevel accessLevel;
    private final LocalDateTime createdAt;

    // 🔒 Private 생성자 - 외부에서 직접 생성 불가
    private DiscussionRoom(Long id, String title, ...) {
        this.id = id;
        this.title = title;
        // ...
    }

    // 🏭 Factory Method: 새 엔티티 생성
    public static DiscussionRoom create(String title, String description,
                                         Region region, AccessLevel accessLevel) {
        validateTitle(title);
        validateDescription(description);
        return new DiscussionRoom(null, title, description, region,
                                   accessLevel, LocalDateTime.now());
    }

    // 🔄 Factory Method: DB에서 복원
    public static DiscussionRoom restore(Long id, String title, ...) {
        return new DiscussionRoom(id, title, description, ...);
    }

    // ✅ 도메인 검증 로직
    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(DiscussionRoomErrorCode.INVALID_TITLE);
        }
        if (title.length() > 100) {
            throw new BusinessException(DiscussionRoomErrorCode.TITLE_TOO_LONG);
        }
    }
}
```

**핵심 특징**:
- **JPA 독립적**: `@Entity`, `@Id` 등의 어노테이션 없음
- **불변성**: 모든 필드 `final`, setter 없음
- **팩토리 메서드**:
  - `create()`: 새 엔티티 생성 + 유효성 검증
  - `restore()`: 영속성 계층에서 복원 시 사용
- **캡슐화**: private 생성자로 생성 방법 제한
- **도메인 검증**: 비즈니스 규칙을 도메인 모델 내부에 위치

##### Repository Interface (도메인 계층에 정의)
```java
public interface DiscussionRoomRepository {
    DiscussionRoom save(DiscussionRoom discussionRoom);
    Optional<DiscussionRoom> findById(Long id);
    List<DiscussionRoom> findLatestRooms(int page, int size);
    void delete(DiscussionRoom discussionRoom);
}
```

**특징**:
- 인터페이스만 정의 (구현은 infra 계층)
- 도메인 모델 타입 사용 (Entity 타입 아님)
- **의존성 역전 원칙**: 도메인이 인프라에 의존하지 않음

#### 4️⃣ Infrastructure Layer
**역할**: 기술적 세부사항 구현 (DB, 캐시, 외부 API 등)

##### JPA Entity (영속성)
```java
@Entity
@Table(name = "discussion_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiscussionRoomEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessLevel accessLevel;

    // 🔄 Domain → Entity 변환 (저장 시)
    public static DiscussionRoomEntity fromDomain(DiscussionRoom domain) {
        DiscussionRoomEntity entity = new DiscussionRoomEntity();
        entity.id = domain.getId();
        entity.title = domain.getTitle();
        entity.description = domain.getDescription();
        entity.region = domain.getRegion();
        entity.accessLevel = domain.getAccessLevel();
        return entity;
    }

    // 🔄 Entity → Domain 변환 (조회 시)
    public DiscussionRoom toDomain() {
        return DiscussionRoom.restore(
            this.id,
            this.title,
            this.description,
            this.region,
            this.accessLevel,
            this.getCreatedAt()
        );
    }
}
```

##### Repository Implementation
```java
@Repository
@RequiredArgsConstructor
public class DiscussionRoomRepositoryImpl implements DiscussionRoomRepository {

    private final DiscussionRoomJpaRepository jpaRepository;

    @Override
    public DiscussionRoom save(DiscussionRoom domain) {
        // Domain → Entity 변환
        DiscussionRoomEntity entity = DiscussionRoomEntity.fromDomain(domain);

        // JPA 저장
        DiscussionRoomEntity saved = jpaRepository.save(entity);

        // Entity → Domain 변환하여 반환
        return saved.toDomain();
    }

    @Override
    public Optional<DiscussionRoom> findById(Long id) {
        return jpaRepository.findById(id)
            .map(DiscussionRoomEntity::toDomain);
    }
}
```

**특징**:
- **매퍼 패턴**: `fromDomain()`, `toDomain()` 양방향 변환
- Spring Data JPA 활용: `JpaRepository` 상속
- 도메인 인터페이스 구현

---

## 도메인 주도 설계 (DDD)

### Domain 구조

현재 4개의 Bounded Context:

```
domain/
├── user/              # 사용자 컨텍스트
├── discussionRoom/    # 논의방 컨텍스트
├── message/           # 메시지 컨텍스트
└── proposal/          # 제안 컨텍스트 (미구현)
```

### 도메인별 책임

#### 1. User Domain
**책임**: 사용자 인증, 회원가입, 이메일 인증

**주요 컴포넌트**:
- `User` (Domain Model): 사용자 엔티티
- `UserRepository`: 사용자 저장소
- `EmailNotifier` (Interface): 이메일 발송 추상화
  - `SmtpEmailNotifier` (Implementation): SMTP 구현체

**특이사항**:
- **Port & Adapter 패턴** 적용
- `EmailNotifier` 인터페이스를 도메인 계층에 정의
- SMTP 구현체는 infra/notification에 위치

#### 2. Discussion Room Domain
**책임**: 논의방 생성/조회/삭제, 멤버 관리

**주요 컴포넌트**:
- `DiscussionRoom` (Aggregate Root): 논의방
- `Member`: 논의방 참여자
- `Region`, `AccessLevel`: Value Object (Enum)
- `DiscussionRoomCacheRepository`: Redis 캐시 전략

**특이사항**:
- **가장 복잡한 캐싱 전략** 적용
- Write-Through + Cache-Aside 패턴
- Soft Delete 지원

#### 3. Message Domain
**책임**: 실시간 채팅, 메시지 저장/조회

**주요 컴포넌트**:
- `Message` (Domain Model): 채팅 메시지
- `MessageType`: JOIN, CHAT (Enum)
- `RedisPublisher`: 메시지 발행
- `RedisSubscriber`: 메시지 구독 → WebSocket 브로드캐스팅

**특이사항**:
- **Redis Pub/Sub + WebSocket 통합**
- 다중 서버 환경 지원
- 커서 기반 페이징

---

## 헥사고날 아키텍처

### Port & Adapter Pattern

```
┌─────────────────────────────────────────────────────────┐
│                    Domain (Core)                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │  Domain Models (User, DiscussionRoom, Message)  │   │
│  └─────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────┐   │
│  │         Ports (Interfaces)                       │   │
│  │  - UserRepository                                │   │
│  │  - DiscussionRoomRepository                      │   │
│  │  - EmailNotifier                                 │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
              ↑                            ↑
              │                            │
    Inbound Adapters              Outbound Adapters
              │                            │
┌─────────────────────┐      ┌──────────────────────────┐
│  REST Controllers   │      │  JPA Repositories        │
│  WebSocket Handler  │      │  Redis Cache             │
│                     │      │  SMTP Email Sender       │
└─────────────────────┘      └──────────────────────────┘
```

### Inbound Ports (주도하는 어댑터)
- `UserController`, `DiscussionRoomController`: REST API
- `MessageController`: WebSocket Handler

### Outbound Ports (주도당하는 어댑터)
- `*Repository` 인터페이스 → `*RepositoryImpl` (JPA)
- `EmailNotifier` 인터페이스 → `SmtpEmailNotifier`
- `DiscussionRoomCacheRepository` (Redis)

**장점**:
- 도메인 로직이 인프라 기술에 독립적
- 테스트 시 Mock 구현체로 교체 가능
- 기술 스택 변경 용이 (예: JPA → MyBatis, SMTP → SendGrid)

---

## 인프라 구조

### Database Architecture

#### PostgreSQL (주 저장소)
```
gyeonggi_partners_db
├── users               # 사용자 테이블
├── discussion_rooms    # 논의방 테이블
├── members             # 논의방 멤버 테이블
└── messages            # 메시지 테이블
```

**특징**:
- Flyway 마이그레이션: `src/main/resources/db/migration/V1__init.sql`
- JPA Auditing: `BaseEntity`를 통한 자동 타임스탬프
- Soft Delete: `deletedAt` 컬럼으로 논리 삭제

#### Redis (캐시 + Pub/Sub)

##### 1. 캐싱 전략 (Discussion Room)
```
Redis Keys:
├── room:{id}                    # Hash - 논의방 상세 정보 (TTL: 24h)
├── list:latest                  # ZSet - 최신 논의방 목록 (TTL: 1h)
├── user:{userId}:joined         # ZSet - 사용자 참여 방 목록 (TTL: 12h)
└── room:{roomId}:members        # List - 논의방 멤버 목록
```

**Cache-Aside (읽기)**:
```java
public Optional<DiscussionRoom> findById(Long id) {
    // 1. 캐시 확인
    Optional<DiscussionRoom> cached = cacheRepository.findById(id);
    if (cached.isPresent()) {
        return cached; // Cache Hit
    }

    // 2. DB 조회
    Optional<DiscussionRoom> fromDb = jpaRepository.findById(id);

    // 3. 캐시 저장
    fromDb.ifPresent(cacheRepository::save);

    return fromDb;
}
```

**Write-Through (쓰기)**:
```java
public DiscussionRoom save(DiscussionRoom domain) {
    // 1. DB 저장
    DiscussionRoom saved = jpaRepository.save(domain);

    // 2. 캐시 즉시 업데이트
    cacheRepository.save(saved);

    return saved;
}
```

##### 2. Pub/Sub (Message Broadcasting)
```
Channel: "chatChannel"

Publisher (MessageService):
  메시지 저장 → Redis Publish

Subscriber (RedisSubscriber):
  Redis Subscribe → SimpMessagingTemplate → WebSocket 클라이언트들
```

**멀티 서버 환경 지원**:
```
Client A → Server 1 → Redis Publish → "chatChannel"
                           ↓
            ┌──────────────┼──────────────┐
            ↓              ↓              ↓
        Server 1       Server 2       Server 3
            ↓              ↓              ↓
        Client A       Client B       Client C
```

### WebSocket Architecture

#### STOMP Protocol
```
WebSocket Endpoint: /gyeonggi_partners-chat
Protocol: STOMP over SockJS

Client → Server:
  - /app/chat.sendMessage    → MessageController.sendMessage()
  - /app/chat.addUser        → MessageController.addUser()

Server → Client:
  - /topic/room/{roomId}     → 해당 방 구독자들에게 브로드캐스트
```

#### 메시지 흐름
```
1. Client: WebSocket 연결 (/gyeonggi_partners-chat)
2. Client: 구독 (/topic/room/1)
3. Client: 메시지 전송 (/app/chat.sendMessage)
4. Server: MessageController.sendMessage()
5. Server: MessageService.processChatMessage()
6. Server: DB 저장 + Redis Publish
7. RedisSubscriber: 메시지 수신
8. RedisSubscriber: SimpMessagingTemplate.convertAndSend()
9. All Clients: /topic/room/1 구독자들에게 메시지 전달
```

**설정 (WebSocketConfig.java)**:
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 엔드포인트 등록
        registry.addEndpoint("/gyeonggi_partners-chat")
                .withSockJS();  // SockJS 폴백 지원
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트 → 서버 (application destination prefix)
        registry.setApplicationDestinationPrefixes("/app");

        // 서버 → 클라이언트 (broker destination prefix)
        registry.enableSimpleBroker("/topic");
    }
}
```

---

## 보안 아키텍처

### JWT 기반 인증

#### 토큰 구조
```
Access Token:
  - 유효기간: 1시간 (3600000ms)
  - Claim: userId, email, authorities
  - 용도: API 요청 인증

Refresh Token:
  - 유효기간: 7일 (604800000ms)
  - 저장: Redis (key: "RT:{userId}", value: refresh token)
  - 용도: Access Token 재발급
```

#### 인증 흐름
```
1. 로그인 (POST /api/auth/login)
   → AuthService.login()
   → JwtTokenProvider.generateTokens()
   → Response: { accessToken, refreshToken }

2. API 요청 (Authorization: Bearer {accessToken})
   → JwtAuthenticationFilter
   → JwtTokenProvider.validateToken()
   → JwtTokenProvider.getAuthentication()
   → SecurityContext에 Authentication 설정
   → Controller 실행 (@AuthenticationPrincipal로 사용자 정보 획득)

3. 토큰 갱신 (POST /api/auth/refresh)
   → AuthService.refresh(refreshToken)
   → Redis에서 Refresh Token 검증
   → 새 Access Token 발급
```

#### JwtAuthenticationFilter
```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) {
    // 1. Header에서 JWT 추출
    String token = resolveToken(request);

    // 2. 토큰 유효성 검증
    if (token != null && jwtTokenProvider.validateToken(token)) {
        // 3. 인증 객체 생성
        Authentication auth = jwtTokenProvider.getAuthentication(token);

        // 4. SecurityContext에 저장
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    filterChain.doFilter(request, response);
}
```

### Spring Security Configuration

**SecurityConfigDev.java** (개발 환경):
```java
@Configuration
@EnableWebSecurity
@Profile("!prod")
public class SecurityConfigDev {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .csrf(disable)                    // CSRF 비활성화 (JWT 사용)
            .sessionManagement(STATELESS)     // 세션 사용 안 함
            .addFilterBefore(                 // JWT 필터 추가
                new JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter.class
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/discussion-rooms/**").authenticated()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().permitAll()  // 개발 환경: 나머지 허용
            );

        return http.build();
    }
}
```

### CORS 설정
- **개발 환경**: 모든 origin 허용 (`allowedOriginPatterns: *`)
- **운영 환경**: 특정 도메인만 허용 (prod 프로파일에서 설정)

---

## 데이터 흐름

### 1. 논의방 생성 흐름
```
Client
  │ POST /api/discussion-rooms/create
  │ { title, description, region, accessLevel }
  ↓
DiscussionRoomController
  │ @AuthenticationPrincipal로 userId 획득
  ↓
DiscussionRoomService (Application Layer)
  │ 1. DiscussionRoom.create() - 도메인 객체 생성 + 검증
  │ 2. discussionRoomRepository.save() - 저장
  │ 3. cacheRepository.saveNewRoomToRedis() - 캐시 업데이트
  │ 4. Member.create(userId, roomId) - 생성자 자동 입장
  │ 5. memberRepository.save()
  ↓
DiscussionRoomRepositoryImpl (Infrastructure Layer)
  │ 1. DiscussionRoomEntity.fromDomain(domain) - Domain → Entity
  │ 2. jpaRepository.save(entity) - JPA 저장
  │ 3. entity.toDomain() - Entity → Domain
  ↓
DiscussionRoomCacheRepository
  │ redisTemplate.opsForHash().putAll("room:{id}", data)
  │ redisTemplate.opsForZSet().add("list:latest", roomId, timestamp)
  ↓
PostgreSQL + Redis
  │ 데이터 영속화 + 캐시 저장
  ↓
Response
  │ JoinRoomRes { room, members }
```

### 2. 실시간 채팅 메시지 흐름
```
Client A (WebSocket)
  │ SEND /app/chat.sendMessage
  │ { roomId, senderId, content, type: "CHAT" }
  ↓
MessageController
  │ @MessageMapping("/chat.sendMessage")
  ↓
MessageService
  │ 1. Message.create() - 도메인 객체 생성
  │ 2. messageRepository.save() - DB 저장
  │ 3. redisPublisher.publish("chatChannel", message) - Redis Pub
  ↓
Redis Pub/Sub Channel: "chatChannel"
  │ 메시지 발행
  ↓
RedisSubscriber (Server 1, 2, 3... 모두 수신)
  │ handleMessage(String message)
  │ 1. JSON 역직렬화
  │ 2. simpMessagingTemplate.convertAndSend(
  │      "/topic/room/{roomId}",
  │      messageResponse
  │    )
  ↓
All Connected Clients (구독자들)
  │ SUBSCRIBE /topic/room/{roomId}
  │ ← 메시지 수신
```

---

## 주요 디자인 패턴

### 1. Factory Pattern (팩토리 메서드)
```java
// 도메인 객체 생성을 캡슐화
public static DiscussionRoom create(String title, ...) {
    validateTitle(title);
    return new DiscussionRoom(null, title, ...);
}

public static DiscussionRoom restore(Long id, ...) {
    return new DiscussionRoom(id, title, ...);
}
```

### 2. Repository Pattern
```java
// 도메인 계층에 인터페이스 정의
public interface DiscussionRoomRepository {
    DiscussionRoom save(DiscussionRoom room);
}

// 인프라 계층에 구현
@Repository
public class DiscussionRoomRepositoryImpl implements DiscussionRoomRepository {
    // JPA, Redis 등 기술적 세부사항
}
```

### 3. Mapper Pattern (양방향 변환)
```java
// Entity → Domain
public DiscussionRoom toDomain() { ... }

// Domain → Entity
public static DiscussionRoomEntity fromDomain(DiscussionRoom domain) { ... }
```

### 4. Strategy Pattern (캐싱 전략)
```java
// Write-Through
public DiscussionRoom save(DiscussionRoom room) {
    room = dbRepository.save(room);        // 전략 1: DB 저장
    cacheRepository.save(room);            // 전략 2: 캐시 저장
    return room;
}

// Cache-Aside
public Optional<DiscussionRoom> findById(Long id) {
    return cacheRepository.findById(id)    // 전략 1: 캐시 조회
        .or(() -> {
            Optional<DiscussionRoom> room = dbRepository.findById(id);
            room.ifPresent(cacheRepository::save); // 전략 2: 캐시 미스 시 저장
            return room;
        });
}
```

### 5. Template Method Pattern (BaseEntity)
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}

// 모든 Entity가 상속
@Entity
public class DiscussionRoomEntity extends BaseEntity { ... }
```

### 6. Adapter Pattern (Port & Adapter)
```java
// Port (인터페이스)
public interface EmailNotifier {
    void sendVerificationEmail(String to, String code);
}

// Adapter (구현체)
@Component
public class SmtpEmailNotifier implements EmailNotifier {
    private final JavaMailSender mailSender;

    @Override
    public void sendVerificationEmail(String to, String code) {
        // SMTP 구현
    }
}
```

### 7. Publisher-Subscriber Pattern (Redis Pub/Sub)
```java
// Publisher
@Component
public class RedisPublisher {
    public void publish(String channel, Object message) {
        redisTemplate.convertAndSend(channel, message);
    }
}

// Subscriber
@Component
public class RedisSubscriber {
    public void handleMessage(String message) {
        // 구독자들에게 브로드캐스트
        simpMessagingTemplate.convertAndSend("/topic/room/" + roomId, data);
    }
}
```

### 8. Exception Handling Pattern (Centralized)
```java
// 도메인 예외 정의
public enum DiscussionRoomErrorCode implements ErrorCode {
    ROOM_NOT_FOUND(404, "R001", "존재하지 않는 논의방입니다.");
}

// 비즈니스 예외 발생
throw new BusinessException(DiscussionRoomErrorCode.ROOM_NOT_FOUND);

// 중앙 집중식 예외 처리
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handle(BusinessException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
            .body(ApiResponse.error(e.getErrorCode()));
    }
}
```

---

## 디렉토리 구조 요약

```
src/main/java/org/example/gyeonggi_partners/
├── common/                          # 공통 모듈
│   ├── dto/                         # 공통 DTO (ApiResponse, ErrorResponse)
│   ├── exception/                   # 예외 인프라
│   │   ├── ErrorCode.java           # 에러 코드 인터페이스
│   │   ├── BusinessException.java   # 비즈니스 예외
│   │   └── GlobalExceptionHandler.java
│   └── jwt/                         # JWT 인증 인프라
│       ├── JwtTokenProvider.java
│       ├── JwtAuthenticationFilter.java
│       └── CustomUserDetailsService.java
│
├── config/                          # 설정 클래스
│   ├── SecurityConfigDev.java       # Spring Security
│   ├── WebSocketConfig.java         # WebSocket/STOMP
│   ├── RedisConfig.java             # Redis
│   └── JpaConfig.java               # JPA Auditing
│
└── domain/                          # 도메인 모듈
    ├── common/
    │   └── BaseEntity.java          # 공통 엔티티 속성
    │
    ├── user/
    │   ├── api/
    │   │   ├── UserController.java
    │   │   └── AuthController.java
    │   ├── application/
    │   │   ├── UserService.java
    │   │   └── AuthService.java
    │   ├── domain/
    │   │   ├── model/User.java
    │   │   ├── repository/UserRepository.java
    │   │   └── notifier/EmailNotifier.java
    │   ├── infra/
    │   │   ├── persistence/
    │   │   │   ├── UserEntity.java
    │   │   │   └── UserRepositoryImpl.java
    │   │   └── notification/
    │   │       └── SmtpEmailNotifier.java
    │   └── exception/
    │       └── UserErrorCode.java
    │
    ├── discussionRoom/
    │   ├── api/DiscussionRoomController.java
    │   ├── application/DiscussionRoomService.java
    │   ├── domain/
    │   │   ├── model/
    │   │   │   ├── DiscussionRoom.java
    │   │   │   ├── Member.java
    │   │   │   ├── Region.java (Enum)
    │   │   │   └── AccessLevel.java (Enum)
    │   │   └── repository/
    │   │       ├── DiscussionRoomRepository.java
    │   │       └── MemberRepository.java
    │   ├── infra/
    │   │   ├── persistence/
    │   │   │   ├── discussionRoom/
    │   │   │   │   ├── DiscussionRoomEntity.java
    │   │   │   │   └── DiscussionRoomRepositoryImpl.java
    │   │   │   └── member/
    │   │   │       ├── MemberEntity.java
    │   │   │       └── MemberRepositoryImpl.java
    │   │   └── cache/
    │   │       └── DiscussionRoomCacheRepository.java
    │   └── exception/DiscussionRoomErrorCode.java
    │
    └── message/
        ├── api/MessageController.java
        ├── application/
        │   ├── MessageService.java
        │   ├── RedisPublisher.java
        │   └── RedisSubscriber.java
        ├── domain/
        │   ├── model/
        │   │   ├── Message.java
        │   │   └── MessageType.java (Enum)
        │   └── repository/MessageRepository.java
        └── infra/
            └── persistence/
                ├── MessageEntity.java
                └── MessageRepositoryImpl.java
```

---

## 참고 자료

- [CLAUDE.md](../CLAUDE.md) - Claude Code 사용 가이드
- [FEATURE.md](./FEATURE.md) - 구현된 기능 설명
- [README.md](./README.md) - 프로젝트 개요
- [docs/backend/README.md](backend/README.md) - 백엔드 기술 스택

---

## 아키텍처 의사결정 기록 (ADR)

### 왜 헥사고날 아키텍처를 선택했는가?
- **도메인 독립성**: 비즈니스 로직이 프레임워크/라이브러리에 종속되지 않음
- **테스트 용이성**: 인터페이스 기반으로 Mock 객체 주입 가능
- **기술 변경 유연성**: JPA → MyBatis, Redis → Memcached 등 교체 용이

### 왜 도메인 모델과 엔티티를 분리했는가?
- **관심사 분리**: 비즈니스 로직 vs 영속성 관리
- **순수성 유지**: 도메인 모델이 JPA 어노테이션으로 오염되지 않음
- **변경 격리**: DB 스키마 변경이 도메인 로직에 영향을 주지 않음

### 왜 Redis Pub/Sub을 사용하는가?
- **수평 확장**: 여러 서버 인스턴스 간 메시지 동기화
- **WebSocket 한계 극복**: 단일 서버 WebSocket은 다른 서버 클라이언트에게 전달 불가
- **실시간성**: Pub/Sub는 낮은 지연시간으로 메시지 전달

### 왜 커서 기반 페이징을 사용하는가?
- **일관성**: Offset 방식은 실시간 데이터 추가/삭제 시 중복/누락 발생 가능
- **성능**: 대량 데이터에서 Offset은 성능 저하 (OFFSET 10000은 10000개 스캔 후 버림)
- **무한 스크롤**: 모바일 앱에 적합한 페이징 방식
