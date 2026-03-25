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












