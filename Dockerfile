# 멀티 스테이지 빌드를 사용한 Spring Boot 애플리케이션
# Stage 1: Build
FROM --platform=linux/amd64 gradle:8.6-jdk21 AS builder

WORKDIR /app

# Gradle 래퍼와 설정 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 다운로드 (캐싱 최적화)
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드 (테스트 제외)
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime
FROM --platform=linux/amd64 eclipse-temurin:21-jre-jammy

WORKDIR /app

# 타임존 설정 (한국 시간) 및 curl 설치 (헬스체크용)
ENV TZ=Asia/Seoul
RUN apt-get update && \
    apt-get install -y --no-install-recommends tzdata curl && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# Firebase 설정 디렉토리 생성 (볼륨 마운트용)
RUN mkdir -p /app/config

# 환경 변수 설정
ENV SPRING_PROFILES_ACTIVE=prod

# 포트 노출
EXPOSE 8008

# # 헬스체크
# HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=3 \
#     CMD curl -f http://localhost:8008/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
