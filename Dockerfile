# ── 1단계: 레이어 분리 (빌더) ──────────────────────
FROM eclipse-temurin:17-jre AS builder
WORKDIR /app

# Fat JAR를 Spring Boot 레이어 구조로 분리
COPY build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ── 2단계: 실행 이미지 ──────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app

# 변경 빈도 낮은 레이어부터 복사 (Docker 캐시 효율 극대화)
# 외부 라이브러리 (거의 안 바뀜)
COPY --from=builder /app/dependencies/ ./
# Spring Boot 로더
COPY --from=builder /app/spring-boot-loader/ ./
# 스냅샷 의존성
COPY --from=builder /app/snapshot-dependencies/ ./
# 실제 작성한 코드 (자주 바뀜)
COPY --from=builder /app/application/ ./

# 컨테이너 실행 시 Spring Boot launcher 직접 호출
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]