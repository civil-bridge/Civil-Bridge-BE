# Discussion Room API 명세 (Refactored)

이 문서는 논의방(Discussion Room) 도메인에 관한 API 명세입니다.

---

## 공통 응답 포맷 (ApiResponse<T>)
모든 API는 아래의 공통 포맷으로 응답합니다.

```json
{
  "code": "SUCCESS",
  "message": "메시지 내용",
  "data": { ... }
}
```

---

## 1. 논의방 관리 API (DiscussionRoomController)

### 1.1 논의방 생성
- **Endpoint**: `POST /api/discussion-rooms/create`
- **Request Body**: `CreateDiscussionRoomReq`
- **Response Data**: `JoinRoomRes`

### 1.2 전체 논의방 목록 조회
- **Endpoint**: `GET /api/discussion-rooms/retrieveTotal`
- **Query Params**: `page` (기본 1), `size` (기본 15)
- **Response Data**: `DiscussionRoomListRes`

### 1.3 내가 참여한 논의방 목록 조회
- **Endpoint**: `GET /api/discussion-rooms/retrieveMyJoined`
- **Query Params**: `page` (기본 1), `size` (기본 15)
- **Response Data**: `DiscussionRoomListRes`

### 1.4 논의방 상세 정보 및 입장
- **Endpoint**: `POST /api/discussion-rooms/{roomId}/join`
- **Response Data**: `JoinRoomRes`

### 1.5 논의방 나가기
- **Endpoint**: `DELETE /api/discussion-rooms/{roomId}/leave`
- **Response Data**: `null`

### 1.6 논의방 삭제 [NEW]
- **Endpoint**: `DELETE /api/discussion-rooms/{roomId}`
- **인증**: 방장(Leader) 권한 필수
- **Response Data**: `null`

### 1.7 참여자 강퇴 [NEW]
- **Endpoint**: `DELETE /api/discussion-rooms/{roomId}/kick/{userId}`
- **인증**: 방장(Leader) 권한 필수
- **Response Data**: `null`

---

## 2. 상세 DTO 명세

### CreateDiscussionRoomReq
```json
{
  "title": "String",           // 필수, 최대 50자
  "description": "String",     // 필수, 최대 200자
  "city": "String",            // 필수 (예: "경기도")
  "district": "String",        // 필수 (예: "부천시")
  "accessLevel": "String"      // 필수, AccessLevel Enum
}
```

### JoinRoomRes (방 상세 및 참여자 정보)
```json
{
  "roomId": 1,
  "title": "부천역 소음 문제 해결",
  "description": "...",
  "city": "경기도",
  "district": "부천시",
  "accessLevel": "PUBLIC",
  "currentUsers": 12,
  "members": [
    {
      "userId": 1,
      "nickname": "김시민",
      "role": "LEADER",
      "profileImageUrl": "https://..."
    },
    {
      "userId": 2,
      "nickname": "홍길동",
      "role": "USER",
      "profileImageUrl": null
    }
  ],
  "joinedAt": "2025-11-08T14:30:00"
}
```

---

## 3. 필드 상세 스펙 및 Enum

### Region (시/도 및 시/군/구) [Updated]
- **구조**: `city` (시/도)와 `district` (시/군/구) 두 단계로 선택합니다.
- **시/도 목록 (City)**:
    - `서울특별시`, `부산광역시`, `대구광역시`, `인천광역시`, `광주광역시`, `대전광역시`, `울산광역시`, `세종특별자치시`, `경기도`, `강원특별자치도`, `충청북도`, `충청남도`, `전북특별자치도`, `전라남도`, `경상북도`, `경상남도`, `제주특별자치도`
- **시/군/구 목록 (District)**:
    - 각 시/도에 종속된 하위 행정구역 (예: 경기도 -> 부천시, 수원시 / 서울특별시 -> 강남구, 서초구 등)

### AccessLevel Enum
`PUBLIC` (전체), `OFFICIALS_ONLY` (공무원), `USER_ONLY` (시민)
