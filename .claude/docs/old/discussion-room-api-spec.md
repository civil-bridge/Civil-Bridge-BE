# DiscussionRoom Domain API 구조 분석

## 개요
Civil Bridge 프로젝트의 DiscussionRoom 도메인은 **1개의 컨트롤러**에 **총 5개의 API 엔드포인트**를 제공합니다.

---

## 1. API 엔드포인트 목록

### DiscussionRoomController (`/api/discussion-rooms`)

| Method | Endpoint | Request DTO | Response DTO | 인증 | 목적 |
|--------|----------|-------------|--------------|------|------|
| POST | `/api/discussion-rooms/create` | `CreateDiscussionRoomReq` | `ApiResponse<JoinRoomRes>` | Bearer | 논의방 생성 |
| GET | `/api/discussion-rooms/retrieveTotal` | Query Params | `ApiResponse<DiscussionRoomListRes>` | Bearer | 전체 논의방 목록 조회 (최신순) |
| GET | `/api/discussion-rooms/retrieveMyJoined` | Query Params | `ApiResponse<DiscussionRoomListRes>` | Bearer | 내가 참여한 논의방 목록 조회 |
| POST | `/api/discussion-rooms/{roomId}/join` | Path Variable | `ApiResponse<JoinRoomRes>` | Bearer | 논의방 입장 |
| DELETE | `/api/discussion-rooms/{roomId}/leave` | Path Variable | `ApiResponse<Void>` | Bearer | 논의방 나가기 |

**참고**: 모든 엔드포인트는 JWT Bearer 인증이 필요합니다.

---

## 2. Request DTO 구조

### 2.1 CreateDiscussionRoomReq
```java
// 논의방 생성 요청
{
  "title": String,         // 필수, 최대 100자, 논의방 제목
  "description": String,   // 선택, 최대 255자, 논의방 설명
  "region": String,        // 필수, Region Enum (28개 경기도 시/군)
  "accessLevel": String    // 필수, AccessLevel Enum (PUBLIC/OFFICIALS_ONLY/USER_ONLY)
}
```

**예시:**
```json
{
  "title": "부천시 BJ로 인한 지역 상권문제",
  "description": "현재 부천시 BJ로 인한 상권 문제에 대해 논의합니다",
  "region": "BUCHEON",
  "accessLevel": "PUBLIC"
}
```

### 2.2 Query Parameters (목록 조회)

**`/api/discussion-rooms/retrieveTotal`**
```java
// 전체 논의방 목록 조회
{
  "page": int,    // 페이지 번호 (1부터 시작), 기본값: 1
  "size": int     // 페이지당 항목 수, 기본값: 15
}
```

**`/api/discussion-rooms/retrieveMyJoined`**
```java
// 내가 참여한 논의방 목록 조회
{
  "page": int,    // 페이지 번호 (1부터 시작), 기본값: 1
  "size": int     // 페이지당 항목 수, 기본값: 15
}
```

### 2.3 Path Parameters

**`/api/discussion-rooms/{roomId}/join`**
```java
// 논의방 입장
{
  "roomId": Long  // 논의방 ID
}
```

**`/api/discussion-rooms/{roomId}/leave`**
```java
// 논의방 나가기
{
  "roomId": Long  // 논의방 ID
}
```

---

## 3. Response DTO 구조

### 3.1 ApiResponse<T> (공통 응답 래퍼)
모든 엔드포인트는 이 구조로 응답합니다:

```java
{
  "code": String,     // "SUCCESS" 또는 에러 코드
  "message": String,  // 응답 메시지
  "data": T          // 실제 데이터 (제네릭)
}
```

### 3.2 JoinRoomRes
```java
// 논의방 생성/입장 응답 (data 필드)
{
  "roomId": Long,                  // 논의방 ID
  "title": String,                 // 논의방 제목
  "description": String,           // 논의방 설명
  "region": String,                // 지역 (Region Enum)
  "accessLevel": String,           // 접근 레벨 (AccessLevel Enum)
  "currentUsers": Integer,         // 현재 참여 인원 수
  "memberNicknames": List<String>, // 참여 멤버 닉네임 목록
  "joinedAt": String              // 입장 시간 (ISO-8601 형식)
}
```

**예시:**
```json
{
  "code": "SUCCESS",
  "message": "논의방에 입장했습니다.",
  "data": {
    "roomId": 1,
    "title": "부천시 BJ로 인한 지역 상권문제",
    "description": "현재 부천시 BJ로 인한 상권 문제에 대해 논의합니다",
    "region": "BUCHEON",
    "accessLevel": "PUBLIC",
    "currentUsers": 15,
    "memberNicknames": ["길동이", "영희", "철수"],
    "joinedAt": "2025-11-08T14:30:00"
  }
}
```

### 3.3 DiscussionRoomListRes
```java
// 논의방 목록 응답 (data 필드)
{
  "rooms": List<DiscussionRoomInfo>,  // 논의방 정보 목록
  "currentPage": int,                 // 현재 페이지 번호
  "pageSize": int,                    // 페이지당 항목 수
  "totalCount": long,                 // 전체 논의방 수
  "totalPages": int                   // 전체 페이지 수
}
```

**예시:**
```json
{
  "code": "SUCCESS",
  "message": "논의방 목록을 조회했습니다.",
  "data": {
    "rooms": [
      {
        "roomId": 1,
        "title": "부천시 BJ로 인한 지역 상권문제",
        "region": "BUCHEON",
        "accessLevel": "PUBLIC",
        "currentUsers": 15,
        "createdAt": "2025-11-06T10:30:00"
      }
    ],
    "currentPage": 1,
    "pageSize": 15,
    "totalCount": 150,
    "totalPages": 10
  }
}
```

### 3.4 DiscussionRoomInfo
```java
// 논의방 정보 (목록용)
{
  "roomId": Long,            // 논의방 ID
  "title": String,           // 논의방 제목
  "region": String,          // 지역 (Region Enum)
  "accessLevel": String,     // 접근 레벨 (AccessLevel Enum)
  "currentUsers": Integer,   // 현재 참여 인원 수
  "createdAt": String       // 생성 시간 (ISO-8601 형식)
}
```

**참고**: 목록 조회 시에는 `description`과 `memberNicknames` 필드가 포함되지 않습니다. 상세 정보는 입장 시 `JoinRoomRes`로 제공됩니다.

---

## 4. 필드 상세 스펙

### CreateDiscussionRoomReq 제약 조건

| 필드 | 타입 | 제약 조건 | 설명 |
|------|------|-----------|------|
| `title` | String | `@NotBlank`, `@Size(max=100)` | 논의방 제목, 필수, 최대 100자 |
| `description` | String | `@Size(max=255)` | 논의방 설명, 선택, 최대 255자 |
| `region` | Region (Enum) | `@NotNull` | 지역, 필수, 28개 경기도 시/군 중 선택 |
| `accessLevel` | AccessLevel (Enum) | `@NotNull` | 접근 레벨, 필수, PUBLIC/OFFICIALS_ONLY/USER_ONLY |

### Region Enum (28개 값)

경기도 28개 시/군:

| 값 | 한글 이름 | 값 | 한글 이름 | 값 | 한글 이름 |
|----|----------|-------|----------|-------|----------|
| `SUWON` | 수원시 | `SEONGNAM` | 성남시 | `UIJEONGBU` | 의정부시 |
| `ANYANG` | 안양시 | `BUCHEON` | 부천시 | `GWANGMYEONG` | 광명시 |
| `PYEONGTAEK` | 평택시 | `DONGDUCHEON` | 동두천시 | `ANSAN` | 안산시 |
| `GOYANG` | 고양시 | `GWACHEON` | 과천시 | `GURI` | 구리시 |
| `NAMYANGJU` | 남양주시 | `OSAN` | 오산시 | `SIHEUNG` | 시흥시 |
| `GUNPO` | 군포시 | `UIWANG` | 의왕시 | `HANAM` | 하남시 |
| `YONGIN` | 용인시 | `PAJU` | 파주시 | `ICHEON` | 이천시 |
| `ANSEONG` | 안성시 | `GIMPO` | 김포시 | `HWASEONG` | 화성시 |
| `GWANGJU` | 광주시 | `YANGJU` | 양주시 | `POCHEON` | 포천시 |
| `YEOJU` | 여주시 | | | | |

### AccessLevel Enum (3개 값)

| 값 | 설명 |
|----|------|
| `PUBLIC` | 모든 사용자 접근 가능 (시민 + 공무원) |
| `OFFICIALS_ONLY` | 공무원만 접근 가능 |
| `USER_ONLY` | 일반 시민만 접근 가능 |

---

## 5. 특이사항 및 비즈니스 로직

### Redis 캐싱 전략
DiscussionRoom 도메인은 4가지 Redis 키 유형을 사용합니다:

1. **논의방 상세 정보**: `room:{roomId}` (Hash, TTL: 24시간)
2. **전체 논의방 목록**: `list:latest` (ZSet, TTL: 1시간)
3. **사용자별 참여 목록**: `user:{userId}:joined` (ZSet, TTL: 12시간)
4. **논의방 멤버 목록**: `room:{roomId}:members` (List, TTL: 24시간)

### Soft Delete
- 마지막 멤버가 나가면 논의방이 자동으로 소프트 삭제됩니다 (`deleted_at` 컬럼 업데이트)
- 소프트 삭제된 논의방은 목록 조회에서 제외됩니다

### 페이지네이션
- 1-based 페이지 번호 사용 (첫 페이지 = 1)
- 기본 페이지 크기: 15
- 전체 논의방: 생성일 기준 최신순 정렬
- 참여한 논의방: 입장일 기준 최신순 정렬

---

## 6. 파일 위치

| 컴포넌트 | 경로                                                                                                 |
|---------|----------------------------------------------------------------------------------------------------|
| DiscussionRoomController | `src/main/java/org/example/civilbridge/domain/discussionRoom/api/DiscussionRoomController.java`    |
| CreateDiscussionRoomReq | `src/main/java/org/example/civilbridge/domain/discussionRoom/api/dto/CreateDiscussionRoomReq.java` |
| DiscussionRoomInfo | `src/main/java/org/example/civilbridge/domain/discussionRoom/api/dto/DiscussionRoomInfo.java`      |
| DiscussionRoomListRes | `src/main/java/org/example/civilbridge/domain/discussionRoom/api/dto/DiscussionRoomListRes.java`   |
| JoinRoomRes | `src/main/java/org/example/civilbridge/domain/discussionRoom/api/dto/JoinRoomRes.java`             |
| Region (Enum) | `src/main/java/org/example/civilbridge/domain/discussionRoom/domain/model/Region.java`             |
| AccessLevel (Enum) | `src/main/java/org/example/civilbridge/domain/discussionRoom/domain/model/AccessLevel.java`        |
| DiscussionRoom (Domain) | `src/main/java/org/example/civ ilbridge/domain/discussionRoom/domain/model/DiscussionRoom.java`    |

---

이 문서는 현재 코드베이스를 기반으로 작성되었습니다. API 구조를 변경할 경우 이 문서도 함께 업데이트해야 합니다.
