# User 도메인 API 마이그레이션

## 목적
기존 API 스펙(old)을 새로운 API 스펙(new)으로 마이그레이션한다.

---

## 명세 파일 경로

- **Old (현재 코드 기준)**: `.claude/docs/old/user-api-spec.md`
- **New (목표 스펙)**: `.claude/docs/new/user-api-spec.md`

---

## 수정 범위

모든 계층을 수정 대상으로 한다:

| 계층 | 경로 | 수정 대상 |
|------|------|----------|
| DTO | `src/main/java/org/example/civilbridge/domain/user/api/dto/` | Request/Response DTO 필드 변경, 신규 DTO 추가 (UpdateProfileRequest, OfficialEmailRequest, OfficialVerifyRequest, OfficialVerifyResponse) |
| Controller | `src/main/java/org/example/civilbridge/domain/user/api/` | 엔드포인트 추가 (회원정보 수정, 공무원 인증) |
| Application | `src/main/java/org/example/civilbridge/domain/user/application/` | Service 로직 (프로필 수정, 공무원 인증 비즈니스 로직) |
| Domain Model | `src/main/java/org/example/civilbridge/domain/user/domain/model/` | User 도메인 모델 (profileImageUrl 필드, 공무원 인증 관련 필드) |
| Domain Repository | `src/main/java/org/example/civilbridge/domain/user/domain/repository/` | UserRepository 인터페이스 (신규 메서드 정의) |
| Infra | `src/main/java/org/example/civilbridge/domain/user/infra/` | UserEntity 수정, UserRepositoryImpl (신규 메서드 구현) |

---

## 작업 절차

1. **명세 비교**: old와 new 명세 파일을 읽고 변경점을 파악한다.
2. **현재 코드 확인**: 수정 범위 내 파일들의 현재 상태를 확인한다.
3. **변경 계획 작성**: 아래 출력 형식에 맞춰 `.claude/docs/migration-plan-user.md` 파일에 작성한다.
4. **승인 대기**: 사용자에게 계획 파일 검토를 요청하고, 승인을 기다린다.
5. **실행**: 사용자가 승인하면 계획대로 코드를 수정한다.

---

## 출력 형식

변경 계획은 `.claude/docs/migration-plan-user.md` 파일에 다음 형식으로 작성한다:

```markdown
# User 마이그레이션 계획

## 요약
- 수정 파일 수: N개
- 신규 파일 수: N개
- 삭제 파일 수: N개

---

## 1. DTO 변경

### 1.1 SignInResponse

**Before:**
​```java
// 현재 코드
​```

**After:**
​```java
// 변경 후 코드 (profileImageUrl 필드 추가)
​```

**변경 사유:**
- 로그인 시 프로필 이미지 URL을 함께 반환하여 클라이언트에서 즉시 표시 가능

---

### 1.2 SignUpRequest

**Before:**
​```java
// 현재 코드
​```

**After:**
​```java
// 변경 후 코드 (profileImageUrl 필드 추가 - Optional)
​```

**변경 사유:**
- 회원가입 시 프로필 이미지 설정 지원

---

### 1.3 UpdateProfileRequest (신규)

**After:**
​```java
// 신규 DTO
​```

**변경 사유:**
- 회원정보 수정 API 지원을 위한 Request DTO

---

### 1.4 OfficialEmailRequest (신규)

**After:**
​```java
// 신규 DTO
​```

**변경 사유:**
- 공무원 인증 메일 발송 API 지원

---

### 1.5 OfficialVerifyRequest (신규)

**After:**
​```java
// 신규 DTO
​```

**변경 사유:**
- 공무원 인증 확인 API 지원

---

### 1.6 OfficialVerifyResponse (신규)

**After:**
​```java
// 신규 DTO
​```

**변경 사유:**
- 공무원 인증 완료 응답 DTO (userId, role, certifiedAt)

---

## 2. Controller 변경

### 2.1 UserController

**추가되는 엔드포인트:**
- `PATCH /api/users/profile` - 회원정보 수정
- `POST /api/users/official/send` - 공무원 인증 메일 발송
- `POST /api/users/official/verify` - 공무원 인증 확인

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

### 3.1 User

**Before:**
​```java
// 현재 User 도메인 모델
​```

**After:**
​```java
// 변경 후 코드 (profileImageUrl, officialEmail, officialCertifiedAt 필드 추가)
​```

**변경 사유:**
- 프로필 이미지 URL 저장
- 공무원 인증 정보 저장 (인증된 공무원 이메일, 인증 시각)

---

## 4. Domain Repository 변경

### 4.1 UserRepository (인터페이스)

**추가되는 메서드:**
- (필요한 쿼리 메서드 정의)

**Before:**
​```java
// 현재 인터페이스
​```

**After:**
​```java
// 변경 후 인터페이스
​```

---

## 5. 신규 파일

| 파일명 | 경로 | 용도 |
|--------|------|------|
| UpdateProfileRequest.java | dto/ | 회원정보 수정 요청 DTO |
| OfficialEmailRequest.java | dto/ | 공무원 인증 메일 발송 요청 DTO |
| OfficialVerifyRequest.java | dto/ | 공무원 인증 확인 요청 DTO |
| OfficialVerifyResponse.java | dto/ | 공무원 인증 확인 응답 DTO |
| (필요시 추가) | | |

---

## 6. Application (Service) 변경

### 6.1 UserService

**추가되는 메서드:**
- `updateProfile(Long userId, UpdateProfileRequest request)` - 회원정보 수정
- `sendOfficialVerificationEmail(Long userId, OfficialEmailRequest request)` - 공무원 인증 메일 발송
- `verifyOfficialEmail(Long userId, OfficialVerifyRequest request)` - 공무원 인증 확인 및 역할 변경

**Before:**
​```java
// 현재 코드 (해당 부분)
​```

**After:**
​```java
// 변경 후 코드
​```

---

## 7. Infra 변경

### 7.1 UserEntity

**추가되는 필드:**
- `profileImageUrl` (String, nullable)
- `officialEmail` (String, nullable)
- `officialCertifiedAt` (LocalDateTime, nullable)

**Before:**
​```java
// 현재 코드
​```

**After:**
​```java
// 변경 후 코드
​```

---

### 7.2 UserRepositoryImpl

**추가되는 메서드:**
- (UserRepository 인터페이스 메서드 구현)

**Before:**
​```java
// 현재 코드
​```

**After:**
​```java
// 변경 후 코드
​```

---

## 8. Redis 변경 (EmailService)

### 8.1 공무원 인증 코드 저장

**추가되는 키 패턴:**
- `official:verification:{email}` (TTL: 5분)

**변경 사유:**
- 기존 이메일 인증과 분리하여 공무원 인증 코드 관리

---

## 9. 주의사항

- 비밀번호 변경 시 `currentPassword` 검증 필수
- 공무원 이메일 도메인 검증 (@go.kr 등 화이트리스트 적용 여부 결정 필요)
- profileImageUrl은 사전에 업로드된 URL을 받으므로, 파일 업로드 API는 별도 도메인에서 처리
- 공무원 인증 완료 시 User의 role이 USER → OFFICIAL로 변경됨
- 기존 API와의 하위 호환성 유지 (기존 필드 삭제 없음)
```

---

## 승인 요청 메시지

계획 작성 완료 후 다음과 같이 사용자에게 요청한다:

> 마이그레이션 계획을 `.claude/docs/migration-plan-user.md`에 작성했습니다.
>
> 파일을 확인하신 후 승인해주시면 코드 수정을 진행하겠습니다.
> - 승인: "승인" 또는 "진행해"
> - 수정 요청: 변경이 필요한 부분을 알려주세요

---

## 참고

- 커밋은 이 작업에 포함하지 않는다. 별도로 `/commit-push` 명령어를 사용한다.
