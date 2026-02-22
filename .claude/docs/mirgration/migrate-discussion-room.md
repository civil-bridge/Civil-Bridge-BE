# DiscussionRoom 도메인 API 마이그레이션

## 목적
기존 API 스펙(old)을 새로운 API 스펙(new)으로 마이그레이션한다.

---

## 명세 파일 경로

- **Old (현재 코드 기준)**: `.claude/docs/old/discussion-room-api-spec.md`
- **New (목표 스펙)**: `.claude/docs/new/discussion-room-api-spec.md`

---

## 수정 범위

모든 계층을 수정 대상으로 한다:

| 계층 | 경로 | 수정 대상 |
|------|------|----------|
| DTO | `src/main/java/org/example/civilbridge/domain/discussionRoom/api/dto/` | Request/Response DTO 필드 변경, 신규 DTO 추가 |
| Controller | `src/main/java/org/example/civilbridge/domain/discussionRoom/api/` | 엔드포인트 추가 (삭제, 강퇴) |
| Application | `src/main/java/org/example/civilbridge/domain/discussionRoom/application/` | Service 로직 (강퇴, 삭제 비즈니스 로직) |
| Domain Model | `src/main/java/org/example/civilbridge/domain/discussionRoom/domain/model/` | Entity, Enum (Region→City/District, 멤버 역할) |
| Infra | `src/main/java/org/example/civilbridge/domain/discussionRoom/infra/` | Repository (필요한 쿼리 메서드 추가) |

---

## 작업 절차

1. **명세 비교**: old와 new 명세 파일을 읽고 변경점을 파악한다.
2. **현재 코드 확인**: 수정 범위 내 파일들의 현재 상태를 확인한다.
3. **변경 계획 작성**: 아래 출력 형식에 맞춰 `.claude/docs/migration-plan-discussion-room.md` 파일에 작성한다.
4. **승인 대기**: 사용자에게 계획 파일 검토를 요청하고, 승인을 기다린다.
5. **실행**: 사용자가 승인하면 계획대로 코드를 수정한다.

---

## 출력 형식

변경 계획은 `.claude/docs/migration-plan-discussion-room.md` 파일에 다음 형식으로 작성한다:

```markdown
# DiscussionRoom 마이그레이션 계획

## 요약
- 수정 파일 수: N개
- 신규 파일 수: N개
- 삭제 파일 수: N개

---

## 1. DTO 변경

### 1.1 CreateDiscussionRoomReq

**Before:**
​```java
// 현재 코드
​```

**After:**
​```java
// 변경 후 코드
​```

**변경 사유:**
- (변경 이유 설명)

---

### 1.2 JoinRoomRes
(동일 형식)

---

## 2. Controller 변경

### 2.1 DiscussionRoomController

**추가되는 엔드포인트:**
- `DELETE /api/discussion-rooms/{roomId}` - 논의방 삭제
- `DELETE /api/discussion-rooms/{roomId}/kick/{userId}` - 참여자 강퇴

**Before:**
​```java
// 현재 코드 (해당 부분)
​```

**After:**
​```java
// 변경 후 코드
​```

---

## 3. Domain Model 변경

### 3.1 Region Enum → City/District

**Before:**
​```java
// 현재 Region Enum
​```

**After:**
​```java
// City Enum 또는 구조 변경안
​```

**변경 사유:**
- 경기도 28개 시/군 → 전국 17개 시/도 + 하위 시/군/구 구조로 확장

---

## 4. 신규 파일

| 파일명 | 경로 | 용도 |
|--------|------|------|
| MemberInfo.java | dto/ | 멤버 정보 DTO (userId, nickname, role, profileImageUrl) |
| (필요시 추가) | | |

---

## 5. Application (Service) 변경

### 5.1 DiscussionRoomService

**추가되는 메서드:**
- `deleteRoom(Long roomId, Long userId)` - 논의방 삭제 (방장 권한 검증)
- `kickMember(Long roomId, Long targetUserId, Long requesterId)` - 참여자 강퇴

**Before:**
​```java
// 현재 코드 (해당 부분)
​```

**After:**
​```java
// 변경 후 코드
​```

---

## 6. Infra (Repository) 변경

### 6.1 DiscussionRoomRepository / MemberRepository

**추가되는 메서드:**
- (필요한 쿼리 메서드)

**Before:**
​```java
// 현재 코드
​```

**After:**
​```java
// 변경 후 코드
​```

---

## 7. 주의사항

- (마이그레이션 시 주의해야 할 점)
- (하위 호환성 이슈 등)
```

---

## 승인 요청 메시지

계획 작성 완료 후 다음과 같이 사용자에게 요청한다:

> 마이그레이션 계획을 `.claude/docs/migration-plan-discussion-room.md`에 작성했습니다.
> 
> 파일을 확인하신 후 승인해주시면 코드 수정을 진행하겠습니다.
> - 승인: "승인" 또는 "진행해"
> - 수정 요청: 변경이 필요한 부분을 알려주세요

---

## 참고

- 커밋은 이 작업에 포함하지 않는다. 별도로 `/commit-push` 명령어를 사용한다.
