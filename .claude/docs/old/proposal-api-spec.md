# Proposal Domain API 구조 분석

## 개요
Civil Bridge 프로젝트의 Proposal 도메인은 **1개의 컨트롤러**에 **총 11개의 API 엔드포인트**를 제공합니다.

**참고**: CLAUDE.md에는 미구현으로 명시되어 있으나, 실제로는 완전히 구현된 협업 제안서 작성 및 투표 시스템입니다.

---

## 1. API 엔드포인트 목록

### ProposalController (`/api/proposals`)

| Method | Endpoint | Request DTO | Response DTO | 인증 | 목적 |
|--------|----------|-------------|--------------|------|------|
| POST | `/api/proposals` | `CreateProposalRequest` | `ApiResponse<ProposalResponse>` | Bearer | 제안서 생성 |
| GET | `/api/proposals/{proposalId}` | Path Variable | `ApiResponse<ProposalResponse>` | Bearer | 제안서 조회 |
| GET | `/api/proposals/rooms/{roomId}` | Path Variable | `ApiResponse<List<ProposalResponse>>` | Bearer | 논의방 내 모든 제안서 조회 |
| POST | `/api/proposals/{proposalId}/start-editing` | Path Variable | `ApiResponse<Void>` | Bearer | 편집 시작 (잠금 획득) |
| POST | `/api/proposals/{proposalId}/finish-editing` | Path Variable | `ApiResponse<Void>` | Bearer | 편집 종료 (잠금 해제) |
| PUT | `/api/proposals/{proposalId}` | `UpdateProposalRequest` | `ApiResponse<ProposalResponse>` | Bearer | 제안서 수정 |
| GET | `/api/proposals/{proposalId}/lock-status` | Path Variable | `ApiResponse<LockStatusResponse>` | Bearer | 잠금 상태 확인 |
| POST | `/api/proposals/{proposalId}/start-voting` | Path Variable | `ApiResponse<ProposalResponse>` | Bearer | 투표 시작 (3일 기본) |
| POST | `/api/proposals/{proposalId}/end-voting` | Path Variable | `ApiResponse<ProposalResponse>` | Bearer | 투표 수동 종료 |
| POST | `/api/proposals/{proposalId}/consents` | Path Variable | `ApiResponse<Void>` | Bearer | 제안서 동의 |
| GET | `/api/proposals/{proposalId}/consenters` | Path Variable | `ApiResponse<ConsenterListResponse>` | Bearer | 동의자 목록 조회 |

**참고**: 모든 엔드포인트는 JWT Bearer 인증이 필요합니다.

---

## 2. Request DTO 구조

### 2.1 CreateProposalRequest
```java
// 제안서 생성 요청
{
  "title": String,           // 제안서 제목
  "paragraph": String,       // 문제 상황 설명
  "image": String,           // 이미지 URL 또는 데이터
  "solution": String,        // 해결 방안
  "expectedEffect": String,  // 기대 효과
  "roomId": Long            // 논의방 ID
}
```

**예시:**
```json
{
  "title": "부천시 BJ 문제 해결을 위한 제안",
  "paragraph": "현재 부천시는 특정 BJ로 인해 지역 상권이 피해를 받고 있습니다.",
  "image": "https://example.com/image.jpg",
  "solution": "지역 상인회와 협력하여 공동 대응책을 마련합니다.",
  "expectedEffect": "지역 상권 활성화 및 주민 만족도 향상",
  "roomId": 1
}
```

### 2.2 UpdateProposalRequest
```java
// 제안서 수정 요청
{
  "title": String,           // 수정된 제목
  "paragraph": String,       // 수정된 문제 설명
  "image": String,           // 수정된 이미지
  "solution": String,        // 수정된 해결 방안
  "expectedEffect": String   // 수정된 기대 효과
}
```

### 2.3 Path Parameters

**제안서 ID 파라미터 (`{proposalId}`)**
```java
// 대부분의 엔드포인트에서 사용
{
  "proposalId": Long  // 제안서 ID
}
```

**논의방 ID 파라미터 (`{roomId}`)**
```java
// GET /api/proposals/rooms/{roomId}
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

### 3.2 ProposalResponse
```java
// 제안서 응답 (data 필드)
{
  "id": Long,                      // 제안서 ID
  "roomId": Long,                  // 논의방 ID
  "authorId": Long,                // 작성자 ID
  "title": String,                 // 제안서 제목
  "contents": ContentFormatDto,    // 제안서 내용 (중첩 객체)
  "status": String,                // 상태 (SUBMITTABLE/UNSUBMITTABLE/VOTING)
  "consents": List<ConsenterDto>,  // 동의자 목록
  "deadline": String,              // 투표 마감일 (ISO-8601)
  "createdAt": String,             // 생성일시 (ISO-8601)
  "updatedAt": String              // 수정일시 (ISO-8601)
}
```

**예시:**
```json
{
  "code": "SUCCESS",
  "message": "제안서를 생성했습니다.",
  "data": {
    "id": 1,
    "roomId": 1,
    "authorId": 123,
    "title": "부천시 BJ 문제 해결을 위한 제안",
    "contents": {
      "paragraph": "현재 부천시는 특정 BJ로 인해 지역 상권이 피해를 받고 있습니다.",
      "image": "https://example.com/image.jpg",
      "solution": "지역 상인회와 협력하여 공동 대응책을 마련합니다.",
      "expectedEffect": "지역 상권 활성화 및 주민 만족도 향상"
    },
    "status": "SUBMITTABLE",
    "consents": [
      {
        "id": 123,
        "nickname": "길동이"
      }
    ],
    "deadline": null,
    "createdAt": "2025-11-10T14:30:00",
    "updatedAt": "2025-11-10T14:30:00"
  }
}
```

### 3.3 ContentFormatDto (제안서 내용)
```java
// ProposalResponse.contents 필드
{
  "paragraph": String,       // 문제 상황 설명
  "image": String,           // 이미지 URL/데이터
  "solution": String,        // 해결 방안
  "expectedEffect": String   // 기대 효과
}
```

### 3.4 ConsenterDto (동의자 정보)
```java
// ProposalResponse.consents 및 ConsenterListResponse.consenters의 요소
{
  "id": Long,          // 사용자 ID
  "nickname": String   // 사용자 닉네임
}
```

### 3.5 ConsenterListResponse
```java
// 동의자 목록 응답 (data 필드)
{
  "totalConsents": int,              // 총 동의 수
  "consenters": List<ConsenterDto>   // 동의자 정보 목록
}
```

**예시:**
```json
{
  "code": "SUCCESS",
  "message": "동의자 목록을 조회했습니다.",
  "data": {
    "totalConsents": 3,
    "consenters": [
      {
        "id": 123,
        "nickname": "길동이"
      },
      {
        "id": 456,
        "nickname": "영희"
      },
      {
        "id": 789,
        "nickname": "철수"
      }
    ]
  }
}
```

### 3.6 LockStatusResponse
```java
// 편집 잠금 상태 응답 (data 필드)
{
  "isLocked": boolean,           // 잠금 여부
  "lockOwnerId": Long,           // 잠금 소유자 ID (null 가능)
  "lockOwnerNickname": String    // 잠금 소유자 닉네임 (null 가능)
}
```

**예시 (잠금 상태):**
```json
{
  "code": "SUCCESS",
  "message": "잠금 상태를 조회했습니다.",
  "data": {
    "isLocked": true,
    "lockOwnerId": 123,
    "lockOwnerNickname": "길동이"
  }
}
```

**예시 (잠금 해제 상태):**
```json
{
  "code": "SUCCESS",
  "message": "잠금 상태를 조회했습니다.",
  "data": {
    "isLocked": false,
    "lockOwnerId": null,
    "lockOwnerNickname": null
  }
}
```

---

## 4. 필드 상세 스펙

### CreateProposalRequest 필드

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `title` | String | Yes | 제안서 제목 |
| `paragraph` | String | Yes | 문제 상황 설명 |
| `image` | String | No | 이미지 URL 또는 Base64 데이터 |
| `solution` | String | Yes | 제안하는 해결 방안 |
| `expectedEffect` | String | Yes | 기대되는 효과 |
| `roomId` | Long | Yes | 제안서가 속한 논의방 ID |

### UpdateProposalRequest 필드

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `title` | String | Yes | 수정된 제목 |
| `paragraph` | String | Yes | 수정된 문제 설명 |
| `image` | String | No | 수정된 이미지 |
| `solution` | String | Yes | 수정된 해결 방안 |
| `expectedEffect` | String | Yes | 수정된 기대 효과 |

### SubmitStatus Enum (3개 값)

| 값 | 설명 |
|----|------|
| `SUBMITTABLE` | 제출 가능 상태 (편집 가능) |
| `UNSUBMITTABLE` | 제출 불가 상태 |
| `VOTING` | 투표 진행 중 (편집 불가) |

---

## 5. 특이사항 및 비즈니스 로직

### 협업 편집 잠금 시스템
- **Pessimistic Lock 사용**: 동시 편집 방지를 위한 비관적 잠금
- **편집 시작**: `POST /api/proposals/{proposalId}/start-editing`으로 잠금 획득
- **편집 종료**: `POST /api/proposals/{proposalId}/finish-editing`으로 잠금 해제
- **잠금 확인**: `GET /api/proposals/{proposalId}/lock-status`로 현재 잠금 상태 조회
- 잠금을 획득하지 않은 사용자는 제안서를 수정할 수 없습니다

### 투표 시스템
- **투표 시작**: `POST /api/proposals/{proposalId}/start-voting`
  - 기본 투표 기간: 3일
  - 투표 시작 시 상태가 `VOTING`으로 변경
  - `deadline` 필드에 마감일 설정
- **투표 종료**: `POST /api/proposals/{proposalId}/end-voting`
  - 수동으로 투표를 조기 종료 가능
  - 마감일 도달 시 자동 종료

### 동의(Consent) 시스템
- **동의 표시**: `POST /api/proposals/{proposalId}/consents`
- **동의자 조회**: `GET /api/proposals/{proposalId}/consenters`
- 동의는 제안서의 지지를 나타내며, 투표와는 별개의 개념
- 동일 사용자의 중복 동의는 방지됨

### 논의방 연결
- 모든 제안서는 특정 논의방(`roomId`)에 속함
- 논의방별 제안서 목록 조회: `GET /api/proposals/rooms/{roomId}`
- 논의방 멤버만 해당 제안서에 접근 가능

### 상태 전이
```
SUBMITTABLE → (투표 시작) → VOTING → (투표 종료) → UNSUBMITTABLE
     ↑                                                    ↓
     └──────────────────── (재편집) ────────────────────┘
```

---

## 6. 파일 위치

| 컴포넌트 | 경로 |
|---------|------|
| ProposalController | `src/main/java/org/example/civilbridge/domain/proposal/api/ProposalController.java` |
| CreateProposalRequest | `src/main/java/org/example/civilbridge/domain/proposal/api/dto/CreateProposalRequest.java` |
| UpdateProposalRequest | `src/main/java/org/example/civilbridge/domain/proposal/api/dto/UpdateProposalRequest.java` |
| ProposalResponse | `src/main/java/org/example/civilbridge/domain/proposal/api/dto/ProposalResponse.java` |
| ContentFormatDto | `src/main/java/org/example/civilbridge/domain/proposal/api/dto/ContentFormatDto.java` |
| ConsenterDto | `src/main/java/org/example/civilbridge/domain/proposal/api/dto/ConsenterDto.java` |
| ConsenterListResponse | `src/main/java/org/example/civilbridge/domain/proposal/api/dto/ConsenterListResponse.java` |
| LockStatusResponse | `src/main/java/org/example/civilbridge/domain/proposal/api/dto/LockStatusResponse.java` |
| SubmitStatus (Enum) | `src/main/java/org/example/civilbridge/domain/proposal/domain/model/SubmitStatus.java` |
| Proposal (Domain) | `src/main/java/org/example/civilbridge/domain/proposal/domain/model/Proposal.java` |

---

## 7. 주요 기능 요약

Proposal 도메인은 다음과 같은 완전한 협업 제안서 시스템을 제공합니다:

1. **CRUD 작업**: 제안서 생성, 조회, 수정
2. **협업 편집**: 비관적 잠금 기반 동시 편집 방지
3. **투표 관리**: 3일 기본 투표 기간, 수동 종료 가능
4. **동의 추적**: 제안서 지지자 관리 및 조회
5. **논의방 통합**: 논의방별 제안서 그룹화
6. **상태 관리**: 3단계 상태 시스템 (SUBMITTABLE/UNSUBMITTABLE/VOTING)

모든 엔드포인트는 JWT 인증으로 보호되며, Clean Architecture 패턴을 따릅니다.

---

이 문서는 현재 코드베이스를 기반으로 작성되었습니다. API 구조를 변경할 경우 이 문서도 함께 업데이트해야 합니다.
