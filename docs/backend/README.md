# 프로젝트 : 경기 파트너스

## 기술 스택

### 백엔드
- **Language**: Java 21
- **Framework**: Spring Boot 3.5.6
- **Security**: Spring Security + JWT
- **Database**: PostgreSQL 15
- **ORM**: Spring Data JPA
- **Migration**: FlywayMigration
- **Real-time**: WebSocket
- **Build Tool**: Gradle (Kotlin DSL)
- **API Docs**: SpringDoc OpenAPI (Swagger)

### DevOps
- **Containerization**: Docker
- **CI/CD**: GitHub Actions
- **Cloud**: AWS
- **Monitoring**: Spring Actuator, AWS CloudWatch

### Development
- **IDE**: IntelliJ IDEA
- **Logging**: Logback (SLF4J)
- **Code Quality**: Lombok



## 환경별 설정

프로젝트는 4개의 환경 설정 파일로 구성되어 있습니다:


| 파일 | 용도 | Git 포함 |
|------|------|----------|
| `application.properties` | 공통 설정 | ✅ |
| `application-local.properties` | 로컬 개발 환경 | ❌ (.gitignore) |
| `application-dev.properties` | 개발 서버 환경 | ✅ |
| `application-prod.properties` | 운영 서버 환경 | ✅ |


> 💡 `application-local.properties`는 각자 로컬에서 생성해야 합니다.

## 깃허브 

### 브랜치 구조
- `main`: 프로덕션 배포용 브랜치
- `develop`: 개발 통합 브랜치
- `feature/*`: 기능 개발용 브랜치

### 브랜치 규칙
- **작업은 절대로 `main`과 `develop` 브랜치에서 직접 하지 않는다**
- 모든 작업은 `feature` 브랜치에서 진행한다
- 기본적으로 `develop` 브랜치에 위치한다
- 
### 커밋 메시지 규칙
- `feat`: 새로운 기능
- `fix`: 버그 수정
- `docs`: 문서 수정
- `refactor`: 코드 리팩토링 (기능 변경 없이 코드 구조 개선)
- `test`: 테스트 코드 추가 또는 수정
- `rename`: 파일/폴더명 수정 또는 이동
- `remove`: 파일 삭제
- `init`: 프로젝트 초기 설정
- `design`: UI/UX 디자인 변경 (CSS, 레이아웃 등)
- `chore`: 빌드 설정, 패키지 매니저 설정, 기타 잡일
- `style`: 코드 포맷팅, 세미콜론 누락 등 (동작에 영향 없는 변경)

## 로컬 환경 세팅


## Swagger

애플리케이션 실행 후 아래 URL로 접속:
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs JSON: http://localhost:8080/v3/api-docs



[백엔드 로컬 환경 세팅 가이드](SETUP.md)를 참고하세요.





















