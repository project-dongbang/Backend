# DongBang Backend

Java 21 · Spring Boot 4.1.1 · PostgreSQL 17 · Docker Compose

현재 로컬 개발 환경과 CI 구성 단계입니다. 인증·업무 기능·서버 배포는 미구현입니다.

## 프로젝트 구조

기능 중심 모듈러 모놀리스 구조를 사용합니다. 전역 `controller`, `service`, `repository` 폴더를 만들지 않고, 기능별 모듈 안에서만 레이어를 나눕니다.

```text
com.dongbang
├─ global/          # 설정, 보안 필터, 공통 응답·예외
├─ identity/        # 사용자, OAuth, JWT·리프레시 토큰
├─ organization/    # 동아리, 회원, 초대, 권한
├─ event/           # 행사, 참가 신청
├─ attendance/      # QR 출석, 출석 정정
├─ finance/         # 회비, 납부 상태, 회계 장부
├─ file/            # S3 영수증 파일
├─ notification/    # 알림
├─ dashboard/       # 여러 도메인을 조합하는 조회 전용 기능
└─ health/          # 상태 확인
```

각 기능 모듈은 필요해질 때 아래처럼 추가합니다. 빈 패키지나 `package-info.java`는 만들지 않습니다.

```text
finance
├─ presentation/    # Controller, 요청·응답 DTO
├─ application/     # 유스케이스, 트랜잭션, 권한 확인
├─ domain/          # Entity, Enum, 업무 규칙
└─ infrastructure/  # JPA, S3 등 기술 구현
```

- Controller는 HTTP 요청·응답만 처리하고, Entity를 직접 반환하지 않습니다.
- Application Service가 유스케이스와 트랜잭션을 관리합니다.
- 다른 기능의 Repository에 직접 접근하지 않고 해당 기능의 Application Service를 사용합니다.
- DB 스키마 변경은 `src/main/resources/db/migration`의 Flyway 마이그레이션으로 관리합니다.

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
