# 1단계: 빌드 스테이지 (Gradle 빌드)
FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle clean build -x test --no-daemon

# 2단계: 실행 스테이지 (Spring Boot 실행)
FROM eclipse-temurin:21-jre
WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 환경변수 설정
ENV SPRING_PROFILES_ACTIVE=dev

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]