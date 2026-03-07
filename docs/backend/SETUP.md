# 로컬 환경 세팅 가이드

## 사전 요구사항

다음 프로그램들이 설치되어 있어야 합니다:

- **Java 21** (JDK)
- **Docker Desktop** (MySQL 실행용)
- **IntelliJ IDEA** (권장)
- **Git**

## 1. 환경 설정 파일 생성

> ⚠️ **중요**: `application-local.properties`는 개인 로컬 환경 설정 파일로, `.gitignore`에 포함되어 Git에 올라가지 않습니다. 따라서 **반드시 직접 생성**해야 합니다.

## 2. 프로젝트 클론

> git clone "https://github.com/NiceLeeMan/civil_bridge"

## 3. 도커 실행하기
- **프로젝트 루트로 이동**: cd civil_bridge 
- **도커 컨테이너 실행**: docker-compose up -d
- **컨테이너 실행 확인**: docker-compose ps
- **컨테이너 완전 종료**: docker-compose down -v

---

## 4. 애플리케이션 실행 플로우

### 🔄 실행 순서 (중요!)

애플리케이션을 실행할 때는 **반드시 아래 순서**를 따라야 합니다:

```
1. Docker Desktop 실행
   ↓
2. docker-compose up -d (PostgreSQL, Redis 시작)
   ↓
3. Spring Boot 애플리케이션 실행
```

### 📋 실행 방법

#### 1. Docker Desktop에서 컨테이너 실행
- Docker Desktop 앱 실행
- 프로젝트 루트에서: `docker-compose up -d`
- Docker Desktop에서 컨테이너 상태 확인

#### 2. 상태 확인
- **Docker**: Docker Desktop에서 이미지(postgres , redis) 상태 확인
- **Spring Boot**: IntelliJ Services 탭(`Alt+8`) 헬스 체크 확인

### 🛑 개발 종료 시

```bash
# Spring Boot: IntelliJ에서 Stop 버튼

# Docker 컨테이너 중지
docker-compose stop

# Docker 컨테이너 + 데이터 완전 삭제
docker-compose down -v
```

### 🔧 문제 해결

#### 연결 실패 시
```bash
# Docker 컨테이너 상태 확인
docker ps

# 컨테이너 재시작
docker-compose restart

# 로그 확인
docker-compose logs mysql-db
docker-compose logs redis-cache
```

#### DB 초기화가 필요한 경우
```bash
docker-compose down -v
rm -rf mysql-data redis-data
docker-compose up -d
```

---

## 추가 정보

- [README.md](../../README.md) - 프로젝트 개요
- [백엔드 아키텍처](README.md) - 아키텍처 설명
