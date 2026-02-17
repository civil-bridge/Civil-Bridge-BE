# User Domain API 구조 분석

## 개요
Civil Bridge 프로젝트의 User 도메인은 **2개의 컨트롤러**에 **총 7개의 API 엔드포인트**를 제공합니다.

---

## 1. API 엔드포인트 목록

### AuthController (`/api/auth`)

| Method | Endpoint | Request DTO | Response DTO | 인증 | 목적 |
|--------|----------|-------------|--------------|------|------|
| POST | `/api/auth/login` | `SignInRequest` | `ApiResponse<SignInResponse>` | Public | 로그인 및 JWT 토큰 발급 |
| POST | `/api/auth/logout` | - | `ApiResponse<Void>` | Bearer | 로그아웃 및 토큰 무효화 |
| POST | `/api/auth/refresh` | `RefreshTokenRequest` | `ApiResponse<TokenDto>` | Public | Refresh Token으로 Access Token 재발급 |

### UserController (`/api/users`)

| Method | Endpoint | Request DTO | Response DTO | 인증 | 목적 |
|--------|----------|-------------|--------------|------|------|
| POST | `/api/users/signup` | `SignUpRequest` | `ApiResponse<SignUpResponse>` | Public | 회원가입 |
| POST | `/api/users/email/send` | `EmailVerificationRequest` | `ApiResponse<Void>` | Public | 이메일 인증코드 발송 |
| POST | `/api/users/email/verify` | `VerifyEmailRequest` | `ApiResponse<Void>` | Public | 이메일 인증코드 검증 |

---

## 2. Request DTO 구조

### 2.1 SignUpRequest
```java
// 회원가입 요청
{
  "loginId": String,      // 6-20자, 영문/숫자만 (^[a-zA-Z0-9]{6,20}$)
  "password": String,     // 8자 이상, 영문+숫자+특수문자(@$!%*#?&) 필수
  "name": String,         // 2-20자, 한글 또는 영문만 (^[가-힣a-zA-Z]{2,20}$)
  "nickname": String,     // 최대 15자, 한글/영문/숫자 (^[가-힣a-zA-Z0-9]{2,15}$)
  "email": String,        // 최대 50자, 이메일 형식
  "phoneNumber": String   // 010-XXXX-XXXX 형식 (^010-\d{4}-\d{4}$)
}
```

### 2.2 SignInRequest
```java
// 로그인 요청
{
  "loginId": String,      // 필수
  "password": String      // 필수
}
```

### 2.3 RefreshTokenRequest
```java
// 토큰 갱신 요청
{
  "refreshToken": String  // Refresh Token
}
```

### 2.4 EmailVerificationRequest
```java
// 이메일 인증코드 발송 요청
{
  "email": String  // 인증할 이메일 주소
}
```

### 2.5 VerifyEmailRequest
```java
// 이메일 인증코드 검증 요청
{
  "email": String,  // 이메일 주소
  "code": String    // 6자리 인증코드
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

**성공 응답 예시:**
```json
{
  "code": "SUCCESS",
  "message": "로그인에 성공했습니다.",
  "data": { ... }
}
```

### 3.2 SignUpResponse
```java
// 회원가입 응답 (data 필드)
{
  "userId": Long,      // 사용자 고유 ID
  "nickname": String,  // 닉네임
  "email": String,     // 이메일
  "role": String       // 역할 (USER, OFFICIAL, ADMIN)
}
```

### 3.3 SignInResponse
```java
// 로그인 응답 (data 필드)
{
  "userId": Long,           // 사용자 고유 ID
  "nickname": String,       // 닉네임
  "email": String,          // 이메일
  "role": String,           // 역할 (USER, OFFICIAL, ADMIN)
  "grantType": String,      // "Bearer"
  "accessToken": String,    // JWT Access Token (1시간 유효)
  "refreshToken": String    // JWT Refresh Token (7일 유효)
}
```

### 3.4 TokenDto
```java
// 토큰 갱신 응답 (data 필드)
{
  "grantType": String,      // "Bearer"
  "accessToken": String,    // 새로운 Access Token (1시간 유효)
  "refreshToken": String    // 새로운 Refresh Token (7일 유효)
}
```

---

## 4. 필드 상세 스펙

### SignUpRequest 제약 조건

| 필드 | 타입 | 제약 조건 | 설명 |
|------|------|-----------|------|
| `loginId` | String | `@NotBlank`, `@Pattern("^[a-zA-Z0-9]{6,20}$")`, `@Size(min=6, max=20)` | 영문/숫자만, 6-20자 |
| `password` | String | `@NotBlank`, `@Pattern("^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$")`, `@Size(min=8, max=50)` | 최소 8자, 영문+숫자+특수문자 필수 |
| `name` | String | `@NotBlank`, `@Pattern("^[가-힣a-zA-Z]{2,20}$")`, `@Size(min=2, max=20)` | 한글 또는 영문, 2-20자 |
| `nickname` | String | `@NotBlank`, `@Pattern("^[가-힣a-zA-Z0-9]{2,15}$")`, `@Size(max=15)` | 한글/영문/숫자, 최대 15자 |
| `email` | String | `@NotBlank`, `@Email`, `@Pattern("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,30}$")`, `@Size(max=50)` | 이메일 형식, 최대 50자 |
| `phoneNumber` | String | `@NotBlank`, `@Pattern("^010-\\d{4}-\\d{4}$")`, `@Size(max=20)` | 010-XXXX-XXXX 형식 |

---

## 5. 인증 흐름

### JWT 토큰 관리
- **Access Token**: 1시간 만료, 보호된 엔드포인트 접근용
- **Refresh Token**: 7일 만료, Redis에 `RT:{userId}` 키로 저장
- **토큰 검증**: `JwtAuthenticationFilter`를 통해 검증

### 공개 엔드포인트 (토큰 불필요)
- `/api/auth/login`
- `/api/users/signup`
- `/api/users/email/send`
- `/api/users/email/verify`
- `/api/auth/refresh`

### 보호된 엔드포인트 (Bearer 토큰 필요)
- `/api/auth/logout`

---

## 6. 파일 위치

| 컴포넌트 | 경로 |
|---------|------|
| AuthController | `src/main/java/org/example/civilbridge/domain/user/api/AuthController.java` |
| UserController | `src/main/java/org/example/civilbridge/domain/user/api/UserController.java` |
| SignUpRequest | `src/main/java/org/example/civilbridge/domain/user/api/dto/SignUpRequest.java` |
| SignUpResponse | `src/main/java/org/example/civilbridge/domain/user/api/dto/SignUpResponse.java` |
| SignInRequest | `src/main/java/org/example/civilbridge/domain/user/api/dto/SignInRequest.java` |
| SignInResponse | `src/main/java/org/example/civilbridge/domain/user/api/dto/SignInResponse.java` |
| RefreshTokenRequest | `src/main/java/org/example/civilbridge/domain/user/api/dto/RefreshTokenRequest.java` |
| EmailVerificationRequest | `src/main/java/org/example/civilbridge/domain/user/api/dto/EmailVerificationRequest.java` |
| VerifyEmailRequest | `src/main/java/org/example/civilbridge/domain/user/api/dto/VerifyEmailRequest.java` |
| TokenDto | `src/main/java/org/example/civilbridge/common/dto/TokenDto.java` |
| ApiResponse | `src/main/java/org/example/civilbridge/common/dto/ApiResponse.java` |

---

이 문서는 현재 코드베이스를 기반으로 작성되었습니다. API 구조를 변경할 경우 이 문서도 함께 업데이트해야 합니다.
