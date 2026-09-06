# DongBang Backend

Java 21 · Spring Boot 4.1.1 · PostgreSQL 17 · Docker Compose

현재 로컬 개발 환경과 CI 구성 단계입니다. 인증·업무 기능·서버 배포는 미구현입니다.

## 실행

Docker Desktop을 켜고 **Backend 디렉터리**에서 실행합니다. 아래는 PowerShell 기준입니다.

```powershell
Copy-Item .env.example .env  # 최초 1회만, 기존 파일은 보존
docker compose --profile app up --build -d --wait
.\scripts\Test-Api.ps1
```

- API: `http://localhost:8080/api/health`
- DB: `localhost:5432` — 계정 설정은 `.env` 참고
- 로그: `docker compose --profile app logs -f api`
- 종료: `docker compose --profile app down`

`.env`는 커밋하지 않습니다. `down -v`는 DB 데이터를 삭제하므로 주의하세요.
현재 Compose 설정은 로컬 전용입니다.

## IDE 개발

JDK 21이 필요합니다. DB는 Docker로, API는 IDE 또는 Gradle로 실행합니다.

```powershell
docker compose --profile app stop api
docker compose up -d --wait db
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

IDE에서는 활성 프로필을 `local`로 지정하세요.
DB 설정을 바꿨다면 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`도 맞춰주세요.
Spring Boot는 `.env`를 자동으로 읽지 않습니다.

## 테스트·빌드

```powershell
.\gradlew.bat test bootJar
```

DB 없이 API·보안 테스트를 실행하고 `build/libs/dongbang.jar`를 생성합니다.
DB 통합 테스트는 CI에서 자동으로 실행합니다.

## CI

PR 및 `main`·`develop` 푸시 시 테스트·DB 통합 테스트·JAR·Docker 빌드를 검증합니다.
자동 배포(CD)는 아직 구성하지 않았습니다.
