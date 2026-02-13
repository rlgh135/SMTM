# ============================================================
# 스테이지 1: 빌드 (Gradle + JDK 17)
# ============================================================
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Gradle Wrapper 및 빌드 설정 파일을 먼저 복사 (의존성 캐싱 레이어 활용)
COPY gradlew ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./

# gradlew 실행 권한 부여
RUN chmod +x gradlew

# 의존성만 먼저 다운로드 (소스 변경 시 재다운로드 방지)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 후 실행 가능한 JAR 생성 (테스트 제외)
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# ============================================================
# 스테이지 2: 실행 (JRE 17 - 최소 이미지)
# ============================================================
FROM eclipse-temurin:17-jre-alpine AS runner

WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
