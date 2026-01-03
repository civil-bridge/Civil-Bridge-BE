# TECH_STACK.md

경기 파트너스 프로젝트의 기술 스택을 설명합니다.

## 목차
1. [Core Framework](#core-framework)
2. [Database & Persistence](#database--persistence)
3. [Caching & Messaging](#caching--messaging)
4. [Security & Authentication](#security--authentication)
5. [Real-time Communication](#real-time-communication)
6. [API Documentation](#api-documentation)
7. [Monitoring & DevOps](#monitoring--devops)
8. [Build & Development Tools](#build--development-tools)
9. [Testing](#testing)
10. [Utilities](#utilities)

---

## Core Framework

### Java 21
- **용도**: 프로그래밍 언어
- **버전**: Java 21 (LTS)
- **선택 이유**:
  - 최신 LTS 버전으로 장기 지원 보장
  - Virtual Threads, Pattern Matching 등 최신 기능 활용
  - 성능 및 보안 개선

### Spring Boot 3.5.6
- **용도**: 애플리케이션 프레임워크
- **버전**: 3.5.6
- **선택 이유**:
  - 엔터프라이즈급 애플리케이션 개발에 최적화
  - Auto-configuration으로 빠른 개발
  - 풍부한 생태계와 커뮤니티 지원
- **주요 스타터**:
  - `spring-boot-starter-web`: REST API 개발
  - `spring-boot-starter-data-jpa`: JPA/Hibernate 통합
  - `spring-boot-starter-security`: 보안 기능
  - `spring-boot-starter-validation`: 입력 검증
  - `spring-boot-starter-websocket`: WebSocket 지원
  - `spring-boot-starter-actuator`: 헬스 체크 및 모니터링
  - `spring-boot-starter-mail`: 이메일 발송
  - `spring-boot-starter-data-redis`: Redis 통합

---

## Database & Persistence

### PostgreSQL 15
- **용도**: 주 데이터베이스 (RDBMS)
- **버전**: 15
- **포트**: 5433 (로컬)
- **선택 이유**:
  - 강력한 ACID 트랜잭션 보장
  - 복잡한 쿼리 및 인덱싱 지원
  - JSON/JSONB 데이터 타입 지원
  - 오픈소스 & 무료
- **연결 정보**:
  - 드라이버: `org.postgresql:postgresql` (runtime)
  - URL: `jdbc:postgresql://localhost:5433/gyeonggi_partners_db`

### Spring Data JPA
- **용도**: ORM (Object-Relational Mapping)
- **버전**: Spring Boot 3.5.6에 포함
- **기반 기술**: Hibernate 6.x
- **주요 기능**:
  - Repository 인터페이스 자동 구현
  - JPQL/Criteria API 지원
  - Query Method 자동 생성
  - @EntityListeners를 통한 Auditing
- **설정**:
  - Dialect: `org.hibernate.dialect.PostgreSQLDialect`
  - SQL 포매팅: `hibernate.format_sql=true`

### Flyway Migration
- **용도**: 데이터베이스 스키마 버전 관리
- **버전**: `org.flywaydb:flyway-core`, `org.flywaydb:flyway-database-postgresql`
- **선택 이유**:
  - 스키마 변경 이력 추적
  - 롤백 및 재실행 가능
  - 팀 협업 시 스키마 동기화
- **마이그레이션 파일 위치**: `src/main/resources/db/migration/`
- **네이밍 규칙**: `V{version}__{description}.sql`
  - 예: `V1__init.sql`

### Hypersistence Utils
- **용도**: Hibernate 성능 최적화 유틸리티
- **버전**: `io.hypersistence:hypersistence-utils-hibernate-63:3.7.3`
- **주요 기능**:
  - JSON 타입 매핑 지원
  - 배치 처리 최적화
  - N+1 쿼리 문제 해결 도구

---

## Caching & Messaging

### Redis 7.4
- **용도**: 인메모리 데이터 저장소 (캐시 + Pub/Sub)
- **버전**: 7.4-alpine
- **포트**: 6380 (로컬)
- **선택 이유**:
  - 빠른 읽기/쓰기 성능
  - 다양한 데이터 구조 지원 (String, Hash, List, Set, ZSet)
  - Pub/Sub 메시징 지원 (다중 서버 환경)
  - TTL 자동 만료 기능
- **사용 용도**:
  1. **캐싱**:
     - 논의방 정보 캐싱 (Hash)
     - 논의방 목록 캐싱 (ZSet)
     - 사용자 참여 방 목록 (ZSet)
     - Refresh Token 저장
  2. **Pub/Sub**:
     - 실시간 채팅 메시지 브로드캐스팅
     - 다중 서버 간 메시지 동기화

### Spring Data Redis
- **용도**: Redis 클라이언트 라이브러리
- **버전**: Spring Boot 3.5.6에 포함
- **주요 구성**:
  - `RedisTemplate<String, Object>`: Redis 작업 템플릿
  - `RedisConnectionFactory`: Redis 연결 관리
  - `RedisMessageListenerContainer`: Pub/Sub 리스너
  - `ChannelTopic`: Pub/Sub 채널 정의
- **직렬화 설정**:
  - Key: `StringRedisSerializer`
  - Value: `GenericJackson2JsonRedisSerializer` (JSON 형식)

---

## Security & Authentication

### Spring Security
- **용도**: 인증 및 인가 프레임워크
- **버전**: Spring Boot 3.5.6에 포함
- **주요 기능**:
  - JWT 기반 Stateless 인증
  - BCrypt 비밀번호 암호화
  - CSRF 보호 (REST API에서는 비활성화)
  - CORS 설정
  - URL 기반 권한 제어
- **인증 방식**: Bearer Token (JWT)
- **세션 정책**: STATELESS (세션 사용 안 함)

### JWT (JSON Web Token)
- **용도**: 토큰 기반 인증
- **라이브러리**: `io.jsonwebtoken:jjwt`
  - `jjwt-api:0.12.3` (API)
  - `jjwt-impl:0.12.3` (구현체)
  - `jjwt-jackson:0.12.3` (JSON 직렬화)
- **토큰 종류**:
  - **Access Token**: 1시간 유효 (3600000ms)
  - **Refresh Token**: 7일 유효 (604800000ms)
- **Claims**:
  - `userId`: 사용자 ID
  - `email`: 이메일
  - `authorities`: 권한 목록

### BCrypt Password Encoder
- **용도**: 비밀번호 해싱
- **라이브러리**: Spring Security 내장
- **선택 이유**:
  - Salt 자동 생성
  - 단방향 암호화 (복호화 불가)
  - 무차별 대입 공격(Brute Force) 방어

---

## Real-time Communication

### WebSocket
- **용도**: 양방향 실시간 통신
- **프로토콜**: STOMP over WebSocket
- **라이브러리**: `spring-boot-starter-websocket`
- **선택 이유**:
  - HTTP와 달리 지속적인 연결 유지
  - 서버 → 클라이언트 푸시 가능
  - 낮은 지연시간 (채팅에 적합)
- **엔드포인트**: `/gyeonggi_partners-chat`
- **폴백**: SockJS (WebSocket 미지원 브라우저 대응)

### STOMP (Simple Text Oriented Messaging Protocol)
- **용도**: WebSocket 메시징 프로토콜
- **버전**: Spring Messaging 내장
- **주요 개념**:
  - **Destination Prefix**:
    - `/app`: 클라이언트 → 서버 (애플리케이션 메시지)
    - `/topic`: 서버 → 클라이언트 (브로드캐스트)
  - **Mapping**:
    - `@MessageMapping`: 메시지 핸들러
    - `@SendTo`: 응답 목적지
- **메시징 패턴**: Pub/Sub (발행/구독)

### Redis Pub/Sub
- **용도**: 서버 간 메시지 동기화
- **채널**: `chatChannel`
- **작동 방식**:
  1. Server 1이 메시지를 Redis에 Publish
  2. 모든 서버(1, 2, 3...)가 Subscribe하여 메시지 수신
  3. 각 서버가 자신의 WebSocket 클라이언트들에게 브로드캐스트
- **선택 이유**: 수평 확장 가능한 실시간 메시징 아키텍처

---

## API Documentation

### SpringDoc OpenAPI (Swagger)
- **용도**: REST API 문서 자동 생성
- **라이브러리**: `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0`
- **버전**: 2.7.0
- **선택 이유**:
  - OpenAPI 3.0 스펙 지원
  - Swagger UI 자동 생성
  - Spring Boot 3.x 호환
  - 어노테이션 기반 문서화
- **접속 URL**:
  - Swagger UI: `http://localhost:8080/swagger-ui.html`
  - API Docs JSON: `http://localhost:8080/v3/api-docs`
- **주요 어노테이션**:
  - `@Tag`: API 그룹화
  - `@Operation`: 엔드포인트 설명
  - `@Parameter`: 파라미터 설명
  - `@SecurityRequirement`: 인증 요구사항

---

## Monitoring & DevOps

### Spring Boot Actuator
- **용도**: 애플리케이션 모니터링 및 헬스 체크
- **라이브러리**: `spring-boot-starter-actuator`
- **활성화된 엔드포인트**:
  - `/actuator/health`: 애플리케이션 상태
  - `/actuator/info`: 애플리케이션 정보
- **헬스 체크 대상**:
  - PostgreSQL 데이터베이스
  - Redis 연결 상태
- **선택 이유**:
  - 운영 환경에서 서비스 상태 모니터링
  - CI/CD 파이프라인에서 헬스 체크
  - 로드밸런서 헬스 프로브

### Logback (SLF4J)
- **용도**: 로깅 프레임워크
- **라이브러리**: Spring Boot 기본 포함
- **설정 파일**: `src/main/resources/logback-spring.xml`
- **로그 레벨**:
  - ERROR: 에러 로그
  - WARN: 경고 로그
  - INFO: 정보 로그
  - DEBUG: 디버깅 로그
- **로그 저장 위치**: `./logs/` 디렉토리
- **특징**:
  - 파일 롤링 지원
  - 프로파일별 설정 가능

### Docker
- **용도**: 컨테이너화 및 로컬 개발 환경
- **버전**: Docker Compose v3.8
- **컨테이너**:
  1. **PostgreSQL**:
     - 이미지: `postgres:15`
     - 컨테이너명: `gyeonggi-partners-db`
     - 포트: `5433:5432`
     - 볼륨: `./postgres-data:/var/lib/postgresql/data`
  2. **Redis**:
     - 이미지: `redis:7.4-alpine`
     - 컨테이너명: `gyeonggi-partners-redis`
     - 포트: `6380:6379`
     - 볼륨: `./redis-data:/data`
- **실행 명령**: `docker-compose up -d`

### AWS (예정)
- **용도**: 클라우드 인프라
- **계획된 서비스**:
  - EC2: 애플리케이션 서버
  - RDS PostgreSQL: 관리형 데이터베이스
  - ElastiCache Redis: 관리형 Redis
  - CloudWatch: 로그 및 메트릭 수집
  - S3: 정적 파일 저장

---

## Build & Development Tools

### Gradle 8.14.3
- **용도**: 빌드 도구
- **버전**: 8.14.3
- **스크립트 언어**: Kotlin DSL (`build.gradle.kts`)
- **선택 이유**:
  - Maven보다 빠른 빌드 속도
  - Kotlin DSL로 타입 안전성
  - 증분 빌드 및 빌드 캐시 지원
- **주요 태스크**:
  - `build`: 빌드 및 테스트
  - `bootJar`: 실행 가능한 JAR 생성
  - `bootRun`: 애플리케이션 실행
  - `test`: 테스트 실행

### Lombok
- **용도**: 보일러플레이트 코드 자동 생성
- **라이브러리**: `org.projectlombok:lombok`
- **주요 어노테이션**:
  - `@Getter`, `@Setter`: Getter/Setter 자동 생성
  - `@NoArgsConstructor`: 기본 생성자 생성
  - `@RequiredArgsConstructor`: final 필드 생성자
  - `@Builder`: 빌더 패턴 구현
  - `@Slf4j`: Logger 자동 생성
- **사용 범위**: `compileOnly`, `annotationProcessor`

### Spring Boot DevTools
- **용도**: 개발 생산성 도구
- **라이브러리**: `spring-boot-devtools` (developmentOnly)
- **주요 기능**:
  - 자동 재시작 (코드 변경 감지)
  - LiveReload 지원
  - 캐시 자동 비활성화
- **사용 환경**: 개발 환경만 (배포 시 제외)

### IntelliJ IDEA
- **용도**: IDE (통합 개발 환경)
- **권장 버전**: Ultimate Edition
- **필수 플러그인**:
  - Lombok Plugin
  - Spring Boot Plugin
  - Database Tools

---

## Testing

### JUnit 5 (Jupiter)
- **용도**: 단위 테스트 프레임워크
- **라이브러리**: `spring-boot-starter-test` 포함
- **버전**: JUnit 5.x
- **주요 어노테이션**:
  - `@Test`: 테스트 메서드
  - `@BeforeEach`, `@AfterEach`: 테스트 전후 처리
  - `@DisplayName`: 테스트 이름 지정

### Spring Boot Test
- **용도**: Spring 통합 테스트
- **라이브러리**: `spring-boot-starter-test`
- **주요 어노테이션**:
  - `@SpringBootTest`: Spring 컨텍스트 로드
  - `@WebMvcTest`: Controller 테스트
  - `@DataJpaTest`: Repository 테스트
  - `@MockBean`: Mock 객체 주입

    
### Spring Security Test
- **용도**: Spring Security 테스트 지원
- **라이브러리**: `spring-security-test`
- **주요 기능**:
  - `@WithMockUser`: 가짜 인증 사용자
  - `@WithUserDetails`: 실제 UserDetails 로드
  - Security Context 테스트

---

## Utilities

### Jakarta Validation
- **용도**: 입력 검증 (Bean Validation)
- **라이브러리**: `spring-boot-starter-validation` 포함
- **버전**: Jakarta EE 10
- **주요 어노테이션**:
  - `@NotNull`, `@NotBlank`, `@NotEmpty`
  - `@Size`, `@Min`, `@Max`
  - `@Email`, `@Pattern`
  - `@Valid`: 중첩 객체 검증
- **사용 위치**: DTO (Request 객체)

### Jackson
- **용도**: JSON 직렬화/역직렬화
- **라이브러리**: Spring Boot 기본 포함
- **버전**: 2.x
- **사용 용도**:
  - REST API 요청/응답 변환
  - Redis 데이터 JSON 직렬화
  - STOMP 메시지 변환
- **주요 어노테이션**:
  - `@JsonProperty`: 필드명 매핑
  - `@JsonIgnore`: 직렬화 제외
  - `@JsonFormat`: 날짜 포맷 지정

### Spring Mail
- **용도**: 이메일 발송
- **라이브러리**: `spring-boot-starter-mail`
- **프로토콜**: SMTP
- **사용 용도**:
  - 회원가입 이메일 인증
  - 6자리 인증번호 발송
- **메일 발송 Bean**: `JavaMailSender`

---

## 의존성 버전 요약

| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 21 | 프로그래밍 언어 |
| Spring Boot | 3.5.6 | 프레임워크 |
| PostgreSQL | 15 | RDBMS |
| Redis | 7.4-alpine | 캐시 + Pub/Sub |
| Gradle | 8.14.3 | 빌드 도구 |
| Flyway | Spring Boot 포함 | DB 마이그레이션 |
| JWT (jjwt) | 0.12.3 | 토큰 인증 |
| SpringDoc OpenAPI | 2.7.0 | API 문서화 |
| Hypersistence Utils | 3.7.3 | Hibernate 최적화 |

---

## 기술 선택 기준

### 1. 안정성
- LTS 버전 선택 (Java 21, PostgreSQL 15)
- 검증된 프레임워크 (Spring Boot, Hibernate)
- 활발한 커뮤니티 지원

### 2. 성능
- Redis 캐싱으로 조회 성능 향상
- JPA 최적화 (Hypersistence Utils)
- Gradle 빌드 캐시

### 3. 확장성
- Redis Pub/Sub으로 수평 확장 가능
- Stateless 아키텍처 (JWT)
- 도메인 주도 설계로 모듈 분리

### 4. 개발 생산성
- Spring Boot Auto-configuration
- Lombok 보일러플레이트 제거
- Swagger 자동 문서화
- DevTools 자동 재시작

### 5. 보안
- Spring Security 통합
- BCrypt 비밀번호 암호화
- JWT 토큰 기반 인증
- CORS 설정

---

## 개발 환경 vs 운영 환경

### 개발 환경 (local, dev)
- H2 Database (테스트)
- 모든 Origin CORS 허용
- SQL 로깅 활성화
- 개발자 도구 활성화
- Swagger UI 접근 가능

### 운영 환경 (prod)
- PostgreSQL (AWS RDS)
- Redis (AWS ElastiCache)
- 제한된 CORS 설정
- SQL 로깅 최소화
- Swagger UI 비활성화
- HTTPS 필수

---

## 기술 스택 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│                        Client Layer                          │
│          (Web Browser, Mobile App, Postman)                  │
└─────────────────────────────────────────────────────────────┘
                            ↓ HTTP/WebSocket
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│     Spring MVC (REST) + WebSocket (STOMP) + Swagger UI      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      Security Layer                          │
│          Spring Security + JWT + BCrypt Encoder             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                     Business Layer                           │
│              Service (Application Logic)                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                       Domain Layer                           │
│          Domain Models + Repository Interfaces               │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   Infrastructure Layer                       │
│   Spring Data JPA + Redis Template + JavaMailSender         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────┬──────────────────────┬────────────────┐
│   PostgreSQL 15      │     Redis 7.4        │  SMTP Server   │
│   (Main Database)    │   (Cache + Pub/Sub)  │  (Email)       │
└──────────────────────┴──────────────────────┴────────────────┘
```

---

## 참고 자료

- [build.gradle.kts](../build.gradle.kts) - 전체 의존성 정보
- [application.properties](../src/main/resources/application.properties) - 설정 정보
- [docker-compose.yml](../docker-compose.yml) - 인프라 구성
- [ARCHITECTURE.md](./ARCHITECTURE.md) - 아키텍처 상세 설명
