# DongBang Backend

Java 21 · Spring Boot 4.1.1 · PostgreSQL 17 · Docker Compose

현재 로컬 개발 환경과 CI 구성 단계입니다. 인증·업무 기능·서버 배포는 미구현입니다.

## 프로젝트 구조

기능 중심 모듈러 모놀리스 구조를 사용합니다. 하나의 애플리케이션으로 배포하되, 코드는 업무 기능별 모듈로 나누고 각 모듈 내부에서만 레이어를 분리합니다. 전역 `controller`, `service`, `repository` 패키지는 만들지 않습니다.

```text
com.dongbang
├─ global/          # 전 기능에서 공유하는 설정·응답·예외·보안 기반 코드
│  ├─ config/
│  ├─ exception/
│  ├─ response/
│  └─ security/
├─ auth/            # OAuth 로그인, JWT·리프레시 토큰
├─ user/            # 사용자 계정과 프로필
├─ organization/    # 동아리, 소속 회원, 초대, 운영 권한
├─ event/           # 행사와 참가 신청
├─ attendance/      # QR 출석 세션과 출석 기록
├─ finance/         # 회비 부과·납부 상태와 수입·지출 장부
├─ file/            # 영수증 등 파일 메타데이터와 저장소 연동
├─ notification/    # 알림 생성과 발송
├─ dashboard/       # 여러 모듈 데이터를 조합하는 조회 기능
└─ health/          # 애플리케이션 상태 확인
```

각 기능 모듈은 필요해질 때 아래처럼 추가합니다. 빈 패키지나 `package-info.java`는 만들지 않습니다.

```text
finance
├─ presentation/    # Controller와 HTTP 요청·응답 DTO
├─ application/     # 유스케이스, 트랜잭션, 모듈 공개 인터페이스
├─ domain/          # Entity, 값 객체, Enum, 도메인 규칙, Repository 인터페이스
└─ infrastructure/  # JPA Repository 구현과 외부 시스템 연동
```

- 실제 코드가 생길 때 필요한 패키지만 만들며 빈 패키지나 `package-info.java`는 만들지 않습니다.
- 의존 방향은 `presentation → application → domain`을 지킵니다. `infrastructure`는 안쪽 레이어가 정의한 인터페이스를 구현합니다.
- Controller는 HTTP 변환만 담당하고 Entity를 직접 반환하지 않습니다.
- Application Service가 유스케이스, 트랜잭션, 권한 확인을 담당합니다.
- 다른 모듈의 Entity나 Repository에 직접 접근하지 않고 그 모듈의 `application` 공개 인터페이스를 사용합니다.
- `global`에는 특정 업무 기능의 규칙을 넣지 않습니다.
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
