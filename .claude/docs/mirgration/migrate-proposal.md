# Proposal 도메인 API 마이그레이션

## 목적
기존 API 스펙(old)을 새로운 API 스펙(new)으로 마이그레이션한다.

---

## 명세 파일 경로

- **Old (현재 코드 기준)**: `.claude/docs/old/proposal-api-spec.md`
- **New (목표 스펙)**: `.claude/docs/new/proposal-api-spec.md`

---

## 주요 변경 개요

| 항목 | Old | New |
|------|-----|-----|
| 기본 URL | `/api/proposals` | `/api/discussion-rooms/{roomId}/proposal` |
| 엔드포인트 수 | 11개 | 4개 |
| 리소스 구조 | 독립적 (proposalId로 접근) | 논의방 하위 리소스 (roomId로 접근) |
| 편집 잠금 | Pessimistic Lock 시스템 | 제거 (방장만 수정 가능) |
| 투표 시작 | 별도 API (`/start-voting`) | 최종 제출 API (`/submit`)에 통합 |
| 동의자 조회 | 별도 API (`/consenters`) | `ProposalResponse.voting`에 통합 |

---

## 수정 범위

모든 계층을 수정 대상으로 한다:

| 계층 | 경로 | 수정 대상 |
|------|------|----------|
| DTO | `src/main/java/org/example/civilbridge/domain/proposal/api/dto/` | Request/Response DTO 전면 변경 |
| Controller | `src/main/java/org/example/civilbridge/domain/proposal/api/` | URL 구조 변경, 엔드포인트 축소 |
| Application | `src/main/java/org/example/civilbridge/domain/proposal/application/` | Service 로직 (편집 잠금 제거, 투표 로직 간소화) |
| Domain Model | `src/main/java/org/example/civilbridge/domain/proposal/domain/model/` | Proposal, ContentFormat, SubmitStatus 변경 |
| Infra | `src/main/java/org/example/civilbridge/domain/proposal/infra/` | Repository 메서드 변경 |

---

## 작업 절차

1. **명세 비교**: old와 new 명세 파일을 읽고 변경점을 파악한다.
2. **현재 코드 확인**: 수정 범위 내 파일들의 현재 상태를 확인한다.
3. **변경 계획 작성**: 아래 출력 형식에 맞춰 `.claude/docs/migration-plan-proposal.md` 파일에 작성한다.
4. **승인 대기**: 사용자에게 계획 파일 검토를 요청하고, 승인을 기다린다.
5. **실행**: 사용자가 승인하면 계획대로 코드를 수정한다.

---

## 출력 형식

변경 계획은 `.claude/docs/migration-plan-proposal.md` 파일에 다음 형식으로 작성한다:

```markdown
# Proposal 마이그레이션 계획

## 요약
- 수정 파일 수: N개
- 신규 파일 수: N개
- 삭제 파일 수: N개

---

## 1. API 엔드포인트 변경

### 1.1 URL 구조 변경

**Before:**
| Method | Endpoint |
|--------|----------|
| POST | `/api/proposals` |
| GET | `/api/proposals/{proposalId}` |
| GET | `/api/proposals/rooms/{roomId}` |
| POST | `/api/proposals/{proposalId}/start-editing` |
| POST | `/api/proposals/{proposalId}/finish-editing` |
| PUT | `/api/proposals/{proposalId}` |
| GET | `/api/proposals/{proposalId}/lock-status` |
| POST | `/api/proposals/{proposalId}/start-voting` |
| POST | `/api/proposals/{proposalId}/end-voting` |
| POST | `/api/proposals/{proposalId}/consents` |
| GET | `/api/proposals/{proposalId}/consenters` |

**After:**
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/discussion-rooms/{roomId}/proposal` | 제안서/투표 현황 조회 |
| PATCH | `/api/discussion-rooms/{roomId}/proposal` | 제안서 임시 저장 |
| POST | `/api/discussion-rooms/{roomId}/proposal/submit` | 제안서 최종 제출 (투표 시작) |
| POST | `/api/discussion-rooms/{roomId}/proposal/vote` | 제안서 동의 (투표) |

**변경 사유:**
- 제안서를 논의방의 하위 리소스로 재구성
- 논의방당 하나의 제안서만 존재하도록 단순화
- 편집 잠금 시스템 제거 (방장만 수정 가능으로 단순화)

---

## 2. DTO 변경

### 2.1 ProposalResponse

**Before:**
​```java
{
  "id": Long,
  "roomId": Long,
  "authorId": Long,
  "title": String,
  "contents": {
    "paragraph": String,
    "image": String,
    "solution": String,
    "expectedEffect": String
  },
  "status": String,
  "consents": List<ConsenterDto>,
  "deadline": String,
  "createdAt": String,
  "updatedAt": String
}
​```

**After:**
​```java
{
  "proposalId": Long,
  "title": String,
  "content": {
    "issue": String,
    "solution": String,
    "expectedEffect": String,
    "attachments": List<AttachmentDto>
  },
  "status": String,
  "voting": {
    "minAgreements": Integer,
    "currentAgreements": Integer,
    "deadline": String,
    "isVoted": Boolean
  },
  "updatedAt": String
}
​```

**변경 사유:**
- `paragraph` → `issue`로 필드명 변경 (의미 명확화)
- `image` → `attachments` (다중 첨부 파일 지원)
- 동의자 목록 제거, 투표 정보로 대체
- `authorId`, `roomId`, `createdAt` 필드 제거 (불필요)

---

### 2.2 UpdateProposalReq (구 UpdateProposalRequest)

**Before:**
​```java
{
  "title": String,
  "paragraph": String,
  "image": String,
  "solution": String,
  "expectedEffect": String
}
​```

**After:**
​```java
{
  "title": String,
  "issue": String,
  "solution": String,
  "expectedEffect": String,
  "attachments": List<AttachmentDto>
}
​```

---

### 2.3 SubmitProposalReq (신규)

**After:**
​```java
{
  "minAgreements": Integer,  // 목표 동의 인원 수 (1 이상)
  "deadline": String         // 투표 마감 일시 (ISO-8601)
}
​```

---

### 2.4 삭제되는 DTO

| DTO | 삭제 사유 |
|-----|----------|
| CreateProposalRequest | 제안서 생성이 논의방 생성 시 자동 생성으로 변경 |
| LockStatusResponse | 편집 잠금 시스템 제거 |
| ConsenterListResponse | 동의자 목록이 voting 객체로 통합 |
| ConsenterDto | 동의자 상세 정보 불필요 |

---

## 3. Domain Model 변경

### 3.1 SubmitStatus Enum

**Before:**
​```java
SUBMITTABLE, UNSUBMITTABLE, VOTING
​```

**After:**
​```java
DRAFT,    // 초안 (편집 가능)
VOTING,   // 투표 진행 중
CLOSED    // 투표 종료
​```

---

### 3.2 Proposal (Domain Model)

**Before:**
- 독립적 리소스로 관리
- 편집 잠금 관련 필드 존재 (lockOwnerId 등)

**After:**
- 논의방(DiscussionRoom)에 1:1 관계로 종속
- 편집 잠금 필드 제거
- 투표 관련 필드 추가 (minAgreements)

---

### 3.3 ContentFormat (Value Object)

**Before:**
​```java
{
  paragraph: String,
  image: String,
  solution: String,
  expectedEffect: String
}
​```

**After:**
​```java
{
  issue: String,
  solution: String,
  expectedEffect: String,
  attachments: List<Attachment>
}
​```

---

## 4. Application (Service) 변경

### 4.1 ProposalService

**삭제되는 메서드:**
- `startEditing(Long proposalId, Long userId)` - 편집 잠금 획득
- `finishEditing(Long proposalId, Long userId)` - 편집 잠금 해제
- `getLockStatus(Long proposalId)` - 잠금 상태 조회
- `endVoting(Long proposalId)` - 투표 수동 종료
- `getConsenters(Long proposalId)` - 동의자 목록 조회

**변경되는 메서드:**
- `createProposal()` → 논의방 생성 시 자동 호출로 변경
- `updateProposal()` → 방장 권한 검증 추가
- `startVoting()` → `submitProposal()`로 변경 (투표 설정 포함)
- `addConsent()` → `vote()`로 변경 (단순화)

**추가되는 메서드:**
- `getProposal(Long roomId)` - roomId로 제안서 조회

---

## 5. Infra (Repository) 변경

### 5.1 ProposalRepository

**삭제되는 메서드:**
- `findAllByRoomId(Long roomId)` - 복수 조회 불필요

**변경되는 메서드:**
- `findById(Long proposalId)` → `findByRoomId(Long roomId)`

---

## 6. 신규 파일

| 파일명 | 경로 | 용도 |
|--------|------|------|
| SubmitProposalReq.java | dto/ | 최종 제출 요청 DTO |
| AttachmentDto.java | dto/ | 첨부 파일 정보 DTO |
| VotingInfo.java | dto/ | 투표 현황 DTO (ProposalResponse 내 중첩) |

---

## 7. 삭제 파일

| 파일명 | 경로 | 삭제 사유 |
|--------|------|----------|
| CreateProposalRequest.java | dto/ | 생성 API 제거 |
| LockStatusResponse.java | dto/ | 편집 잠금 시스템 제거 |
| ConsenterListResponse.java | dto/ | 동의자 목록 API 제거 |
| ConsenterDto.java | dto/ | 동의자 상세 정보 불필요 |
| ContentFormatDto.java | dto/ | ContentDto로 대체 |

---

## 8. 주의사항

- 논의방 생성 시 Proposal 자동 생성 로직 추가 필요 (DiscussionRoomService 수정)
- 기존 데이터 마이그레이션: 기존 proposals 테이블 데이터를 새 구조에 맞게 변환 필요
- 편집 권한: 기존 "잠금 획득자"에서 "방장"으로 변경
- API 하위 호환성: 기존 클라이언트가 있다면 버저닝 또는 deprecation 고려
```

---

## 승인 요청 메시지

계획 작성 완료 후 다음과 같이 사용자에게 요청한다:

> 마이그레이션 계획을 `.claude/docs/migration-plan-proposal.md`에 작성했습니다.
>
> 파일을 확인하신 후 승인해주시면 코드 수정을 진행하겠습니다.
> - 승인: "승인" 또는 "진행해"
> - 수정 요청: 변경이 필요한 부분을 알려주세요

---

## 참고

- 커밋은 이 작업에 포함하지 않는다. 별도로 `/commit-push` 명령어를 사용한다.
