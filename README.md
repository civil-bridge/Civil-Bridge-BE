# Civil-Bridge  
> 민-관 협력 기반 지역 문제 해결 플랫폼

<br>

## 📋 프로젝트 개요

> **Civil-Bridge**는 시민의 역할을 수동적인 '문제 제기자'에서 능동적인 '솔루션 파트너'로 전환합니다.
시민이 문제 제기에서 그치지 않고 해결방안을 직접 제안하고 토론할 수 있는 구조를 통해,
민(民)과 관(官)이 함께 정책을 설계하는 협력적 거버넌스를 구현합니다.

<br>
<br>

## 🛠 기술 스택
 
| 분류 | 기술 | 버전 | 용도 |
|------|------|------|------|
| Language | Java | 21 | 메인 개발 언어 |
| Framework | Spring Boot | 3.x | 애플리케이션 프레임워크 |
| ORM | Spring Data JPA | - | 데이터 접근 계층 |
| Database | MySQL | 8.0 | 운영 데이터베이스 |
| Cache | Redis | - | 조회 성능 개선을 위한 캐싱 |
| Realtime | WebSocket | - | 실시간 토론/채팅 |
| Infra | AWS EC2 | - | 애플리케이션 서버 배포 |
| Load Test | nGrinder | - | 부하 테스트 및 성능 측정 |

<br>
<br>

## 🏗 아키텍쳐
<br>

### 🐳 베포 아키텍쳐
<img width="2488" height="1450" alt="image" src="https://github.com/user-attachments/assets/c057ba2c-6b4c-4127-8002-41bb3625b9bc" />

<br>

### 📁 패키지 구조
```
src/main/java/org/example/civilbridge/
├── common/          # JWT, WebSocket, 공통 예외 처리
├── config/          # Spring 설정
└── domain/
    ├── user/            # 회원
    ├── discussionRoom/  # 논의방
    ├── message/         # 채팅 메시지
    └── proposal/        # 제안서
```

각 도메인은 헥사고날 아키텍처 구조를 따릅니다.
```
domain/{도메인}/
├── api/          # Controller, Request/Response DTO
├── application/  # Service (비즈니스 로직)
├── domain/       # Entity, Repository 인터페이스
└── infra/        # JPA 구현체, Redis 어댑터
```
<br>
<br>

## 🗄 ERD 다이어그램 및 테이블 설명
<br>

<img width="1228" height="747" alt="image" src="https://github.com/user-attachments/assets/3c5a39b2-b107-4534-b0fc-b493d053d1c9" />

- `members`는 `users`와 `discussion_rooms` 간 N:M 관계를 해소하는 중간 테이블입니다.  
- `proposals`와 `messages`는 각각 `users`, `discussion_rooms`에 대한 복합 FK를 가집니다.
  
<br>
<br>

## ⚡ 성능 최적화

> 상세 분석은 각 보고서 링크를 참고하세요.

### 1. 복합 인덱스 추가 — `retrieveTotal`

**문제**: `deleted_at IS NULL ORDER BY created_at DESC` 조건에 인덱스 미적용 → 풀스캔 + filesort (rows 9,862)

**원인**: EXPLAIN 결과 `type=ALL`, `Using filesort` 확인

**해결**: `(deleted_at, created_at DESC)` 복합 인덱스 추가 → `type=ref`, filesort 제거

| Vuser | Before TPS | After TPS | Before MTT | After MTT |
|-------|-----------|-----------|------------|-----------|
| 99    | 77.9      | 148.9     | 1,263ms    | 666ms     |
| 198   | 76.5      | 172.9     | 2,531ms    | 1,114ms   |

→ TPS 약 2배 향상, MTT 약 50% 감소

---

<br>

### 2. Redis Pipeline + 트랜잭션 분리 — `retrieveMyJoined`

**문제**: `@Transactional` 내부에서 Redis HGETALL을 방 개수(N)만큼 순차 호출 → 커넥션 점유 시간 증가 → Vuser 198에서 에러율 40.1%

**원인**: DB 커넥션 점유 중 Redis 왕복 N회 발생 → HikariCP 대기열 적체

**해결**: `executePipelined()`로 HGETALL 일괄 실행(N회→1회) + `TransactionTemplate`으로 Redis 호출을 트랜잭션 밖으로 분리

| Vuser | Before TPS | After TPS | Before 에러율 | After 에러율 |
|-------|-----------|-----------|--------------|-------------|
| 99    | 436.3      | 401.7     | 0%           | 0%          |
| 198   | 309.3      | 207.0     | **40.1%**    | **0%**      |

→ Vuser 198 에러율 40.1% → 0% 해소

---
<br>

### 3. 비동기 배치 INSERT — `sendMessage`

**문제**: 메시지 전송 시 요청 스레드에서 DB I/O 4회 동기 실행 → TPS ~440 포화

**원인**: INSERT를 요청 스레드가 직접 처리 → HikariCP 커넥션 처리량이 TPS 상한을 결정

**해결**: Redis List에 LPUSH 후 즉시 반환 → `@Scheduled`가 1초마다 최대 500건 RPOP → JDBC `batchUpdate` 일괄 INSERT. 멤버십 검증도 DB SELECT → Redis ZSet 조회로 대체하여 요청 스레드의 DB 커넥션 사용을 0으로 제거

| Vuser | Before TPS | After TPS | Before MTT | After MTT |
|-------|-----------|-----------|------------|-----------|
| 50    | 447        | 1,048     | 111ms      | 47ms      |
| 99    | 436        | 1,296     | 228ms      | 76ms      |
| 198   | 432        | 1,289     | 461ms      | 154ms     |

→ TPS 약 3배 향상, MTT 약 66% 감소

<br>
<br>

## 🔥 트러블슈팅

### 1. sendMessage 비동기 배치 설계 — 2차 실패 끝에 완성

채팅 메시지 전송 TPS ~440 포화 문제를 해결하기 위해 비동기 배치 구조를 도입했으나, 두 번의 실패를 거쳐 최종 구조에 도달했다.

| 단계 | 문제 | 원인 | 조치 |
|------|------|------|------|
| 1차 실패 | 비동기 배치 적용 후 TPS 오히려 1/3 급락 | Vuser 198 기준 1초에 ~440건 LPUSH → `@Scheduled` 실행 시 500건 `batchUpdate`가 커넥션을 장시간 점유 → 그 사이 멤버십 검증 SELECT가 같은 풀 경합 → HikariCP waiting=184 | 멤버십 검증을 Redis ZSet 조회로 대체 → 요청 스레드 DB 커넥션 사용 제거 |
| 2차 실패 | TPS 150으로 여전히 저조 | 큐 500건 초과 시 요청 스레드에서 `flush()` 직접 호출 → `synchronized` 블로킹으로 비동기 구조 무력화 | 요청 스레드에서 flush 호출 제거, `@Scheduled`에만 위임 |
| 최종 결과 | — | — | TPS 436 → 1,296 (3배 향상) |

### 2. retrieveMyJoined — Vuser 198 에러율 40.1%

| 항목 | 내용 |
|------|------|
| 증상 | Vuser 198에서 TPS 급락 + 에러율 40.1% |
| 원인 | `@Transactional` 내 Redis HGETALL N회 순차 호출 → 커넥션 점유 시간 증가 → HikariCP 풀(10개) 고갈 → 신규 요청이 `connectionTimeout(30초)` 초과 → `SQLTransientConnectionException` 발생 |
| 해결 | Redis Pipeline으로 HGETALL N회→1회 일괄 실행 + `TransactionTemplate`으로 Redis 호출을 트랜잭션 밖으로 분리 → 에러율 40.1% → 0% |

<br>
<br>


## 📡 API 명세

> 전체 API 명세는 [API.md](docs/API.md) 또는 Swagger(`/swagger-ui.html`)를 참고하세요.

| 분류 | Method | Endpoint | 인증 | 설명 |
|------|--------|----------|------|------|
| Auth | POST | `/api/auth/login` | ❌ | 로그인 |
| Auth | POST | `/api/auth/refresh` | ❌ | Access Token 재발급 |
| 논의방 | POST | `/api/discussion-rooms/create` | ✅ | 논의방 생성 |
| 논의방 | GET | `/api/discussion-rooms/retrieveTotal` | ❌ | 전체 논의방 목록 |
| 논의방 | GET | `/api/discussion-rooms/retrieveMyJoined` | ✅ | 내 논의방 목록 |
| 논의방 | POST | `/api/discussion-rooms/{roomId}/join` | ✅ | 논의방 입장 |
| 논의방 | DELETE | `/api/discussion-rooms/{roomId}/leave` | ✅ | 논의방 나가기 |
| 제안서 | POST | `/api/proposals` | ✅ | 제안서 생성 |
| 제안서 | POST | `/api/proposals/{proposalId}/start-voting` | ✅ | 투표 시작 |
| 제안서 | POST | `/api/proposals/{proposalId}/consents` | ✅ | 제안서 동의 |
| 메시지 | GET | `/api/messages/rooms/{roomId}` | ❌ | 메시지 목록 (커서 페이지네이션) |
| WebSocket | SEND | `/app/chat.sendMessage` | - | 채팅 메시지 전송 |
