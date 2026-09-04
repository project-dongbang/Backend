# DongBang 인프라 학습 가이드

## 1. 지금 구축하는 범위

인프라는 AWS 서버 한 대를 만드는 일만 뜻하지 않습니다. 팀원이 동일한 환경에서 실행하고,
변경을 검증하며, 나중에 서버에서 재현 가능하게 실행하는 기반도 포함합니다.

현재 작업은 **로컬 실행 환경 + 자동 검증(CI)** 입니다.
AWS 계정·리소스·도메인, 원격 Git 설정, 배포(CD)는 아직 변경하지 않았습니다.

## 2. Gradle과 Spring Boot

`build.gradle`은 Java 21, Spring Boot 버전, 필요한 라이브러리와 작업을 선언합니다.
Wrapper(`gradlew`, `gradlew.bat`, `gradle/wrapper/*`)는 팀원과 CI가 같은 Gradle 버전을 사용하게 합니다.
배포 ZIP의 SHA-256 체크섬도 지정하여 다운로드 파일이 공식 배포 파일과 일치하는지 확인합니다.

의존성의 역할:

- Web MVC: HTTP 요청을 받아 Controller로 전달합니다.
- Data JPA: Java 객체와 관계형 DB 사이의 접근 계층을 구성합니다. 아직 업무 엔티티는 없습니다.
- PostgreSQL JDBC: Java가 PostgreSQL에 연결할 수 있게 하는 드라이버입니다.
- Flyway: SQL 파일의 적용 순서와 이력을 관리합니다.
- Security: HTTP 요청의 접근 허용/차단을 담당합니다. 로그인 기능 자체는 아직 구현하지 않았습니다.
- Actuator: 애플리케이션의 상태 확인 기능입니다.

`bootJar`는 라이브러리를 포함해 `java -jar`로 실행할 수 있는 `dongbang.jar`를 만듭니다.
로컬에 설치된 기본 Java와 Gradle toolchain은 다른 개념입니다. 팀 표준은 Java 21입니다.

## 3. Dockerfile과 Compose는 다릅니다

**Dockerfile은 API 실행 이미지의 제작법**, **Compose는 여러 컨테이너를 함께 실행하는 설정**입니다.

Dockerfile:

1. JDK 이미지에서 소스를 컴파일하고 JAR를 만듭니다.
2. 실행 전용 JRE 이미지에 JAR만 복사합니다.
3. root가 아닌 `dongbang` 사용자로 실행합니다.

이것이 다단계 빌드입니다. 컴파일 도구와 소스를 최종 실행 이미지에 남기지 않습니다.
`.dockerignore`는 `.git`, `.env`, 로컬 빌드 캐시 등이 Docker 빌드에 전달되는 것을 방지합니다.
환경변수에 넣은 비밀값이 자동으로 암호화되는 것은 아닙니다. 운영 비밀값 관리는 별도로 필요합니다.

Compose:

- 기본 실행: PostgreSQL만 실행합니다. API는 IDE에서 개발할 수 있습니다.
- `--profile app`: PostgreSQL과 API를 함께 실행합니다.
- API 컨테이너는 DB 컨테이너의 서비스 이름 `db`로 접속합니다.
- 호스트의 IDE에서는 `localhost:5432`로 접속합니다.
- 포트를 `127.0.0.1`에 바인딩해 로컬 컴퓨터에서만 접근하도록 합니다.
- API는 DB의 healthcheck가 통과한 뒤 시작합니다.

`postgres-data` 볼륨은 컨테이너를 삭제해도 개발 DB 데이터를 보존합니다.
**볼륨은 백업이 아닙니다.** 디스크 장애·실수에 대비한 별도 백업과 복구 테스트가 필요합니다.
테스트용 Compose는 5433 포트와 임시 저장소를 사용하여 개발 데이터와 분리합니다.

## 4. 설정 파일과 환경변수

`application.yaml`은 공통 설정입니다. DB URL, 계정, 비밀번호에 운영용 기본값을 넣지 않았으므로
환경변수를 누락하면 시작하지 않습니다. 잘못된 DB에 조용히 붙는 일을 줄이기 위해서입니다.

`application-local.yaml`은 `local` 프로필을 명시할 때만 적용됩니다.
여기에 있는 비밀번호는 공개된 로컬 개발 예시이며 실제 서비스 비밀번호로 사용하면 안 됩니다.

`.env.example`은 팀 공유용 예시, `.env`는 개인 로컬 설정입니다.
`.gitignore`는 `.env`를 Git 추적 대상에서 제외하지만, 이미 추적한 비밀을 소급 삭제하지는 않습니다.
실제 비밀이 노출되었다면 파일 삭제만으로 해결되지 않고 해당 비밀을 폐기·교체해야 합니다.

주의: Compose의 `.env`와 Spring의 환경변수는 별개입니다.
Compose로 API를 실행할 때는 `environment`가 DB 변수를 넘겨주고,
IDE 실행은 프로필 또는 IDE/셸 환경변수로 설정합니다.

## 5. Flyway와 ddl-auto=validate

현재 마이그레이션 SQL은 없습니다. 첫 업무 테이블을 만들 때
`src/main/resources/db/migration/V1__create_organizations.sql`처럼 실제 변경부터 추가합니다.
Flyway는 버전 순서대로 SQL을 적용하고 이력과 체크섬을 `flyway_schema_history`에 기록합니다.
이미 공용 DB에 적용한 파일은 수정하지 않고 새 버전의 마이그레이션으로 변경합니다.

JPA의 `ddl-auto=validate`는 엔티티와 스키마 일치 여부를 검사하되 테이블을 자동 변경하지 않습니다.
`open-in-view=false`는 웹 응답을 만드는 동안 예상 밖 DB 조회가 발생하는 것을 줄이는 설정입니다.

## 6. 상태 점검과 보안 기본값

| 주소 | 의미 |
|---|---|
| `GET /api/health` | API 요청/응답 연결 확인. DB 준비를 보장하지 않음 |
| `GET /actuator/health/liveness` | 프로세스의 생존 상태 |
| `GET /actuator/health/readiness` | 요청 처리 준비 상태. DB 연결 검사 포함 |

상태 응답에 DB 주소나 세부 구성 정보를 노출하지 않습니다.
Security는 위 GET 주소만 허용하고 나머지는 차단합니다.
폼 로그인·HTTP Basic은 끄고, 서버 세션은 만들지 않는 정책입니다.
현재는 임시 보안 경계이며 JWT/소셜 로그인/동아리별 인가는 아직 없습니다.
CSRF는 일괄 비활성화하지 않았습니다. 쿠키/토큰 인증 방식을 정한 뒤 정책을 검토해야 합니다.
프론트가 다른 출처에서 API를 호출하는 CORS도 아직 허용하지 않았습니다.
실제 프론트 주소를 정한 뒤 허용 출처를 명시해야 합니다.

## 7. 테스트와 CI

`test`는 DB 없이 API 응답 및 접근 차단 정책을 확인합니다.
`integrationTest`는 실제 PostgreSQL에서 애플리케이션을 시작하고 DB 연결을 포함한 준비 상태를 확인합니다.
두 작업을 분리했으므로 `test`나 `build`만 실행하면 DB 통합 테스트는 포함되지 않습니다.
전체 검증 명령은 `test integrationTest bootJar`이고 CI에서 이를 명시적으로 실행합니다.

### 로컬 DB 통합 테스트 실행

Backend 디렉터리의 별도 PowerShell 터미널에서 실행합니다. Docker Desktop과 JDK 21이 필요합니다.
운영/공용 개발 DB가 아닌 아래 임시 테스트 DB를 사용하세요.

```powershell
docker compose -f compose.test.yaml up -d --wait
$env:DB_URL = 'jdbc:postgresql://localhost:5433/dongbang_test'
$env:DB_USERNAME = 'dongbang_test'
$env:DB_PASSWORD = 'test_only_password'
.\gradlew.bat integrationTest
docker compose -f compose.test.yaml down
Remove-Item Env:DB_URL, Env:DB_USERNAME, Env:DB_PASSWORD
```

테스트 실패 시에도 임시 컨테이너 종료 명령을 실행하세요.
기존 환경변수가 있던 터미널이라면 삭제 대신 원래 값으로 복원해야 합니다.
테스트 DB는 임시 저장소를 사용하므로 종료하면 데이터가 사라지고 개발 DB에는 영향이 없습니다.

### GitHub 자동 검증

GitHub Actions의 실행 조건:

- PR 생성/갱신
- `main`, `develop`에 푸시

CI가 하는 일:

1. 코드를 가져오고 Java 21/Gradle을 준비합니다.
2. CI 전용 임시 PostgreSQL을 준비합니다.
3. API·보안 테스트와 DB 통합 테스트를 실행합니다.
4. JAR와 Docker 이미지를 빌드합니다.
5. Compose 설정을 검사합니다.

CI는 테스트 DB의 공개된 임시 암호만 사용하고 운영 비밀값을 요구하지 않습니다.
저장소 권한은 `contents: read`로 제한하고 checkout의 Git 인증 정보도 남기지 않습니다.
Actions는 커밋 SHA로 고정하며 Dependabot 설정으로 업데이트 PR을 받을 수 있게 했습니다.
컨테이너 태그는 현재 메이저 계열 태그입니다. 운영 릴리스는 검증한 digest로 고정하는 절차를 추가합니다.

**CI 통과는 배포 완료를 뜻하지 않습니다.** 이미지 레지스트리 업로드나 서버 업데이트는 아직 없습니다.
필수 CI 통과·리뷰 승인을 강제하려면 GitHub의 브랜치 규칙을 사용자가 따로 설정해야 합니다.

## 8. AWS 단계에서 추가할 것

우선 다음 정보를 확정합니다: AWS 계정 소유자, 월 예산, 서울 리전 사용 여부,
공용 개발 서버 방식(EC2/Lightsail), 도메인, 배포 승인 담당자.

권장 구축 순서:

1. 계정 MFA와 비용 알림. 예산 알림은 과금을 자동 차단하는 장치가 아님에 유의합니다.
2. 개발/운영 분리 및 VPC/보안 그룹 설계. DB를 인터넷에 공개하지 않습니다.
3. EC2의 Docker 실행 환경 및 HTTPS. SSH를 전 세계에 열지 않고 SSM 등 접근 방식을 결정합니다.
4. RDS PostgreSQL: 비공개 접근, 암호화, 백업 보존, 복구 테스트.
5. 프론트 배포용 S3/CloudFront와 첨부파일용 비공개 S3를 분리합니다.
6. 이미지 레지스트리, GitHub OIDC 역할, 환경별 설정/비밀값 관리를 추가합니다.
7. 개발 환경 자동 배포, 운영 환경 승인 배포, 상태 확인 실패 시 복구 절차를 구성합니다.
8. 로그 보존 기간, 오류 알림, 디스크/메모리 관측, 데이터 백업 복구를 점검합니다.

첫 운영 서버 한 대는 단일 장애 지점입니다. 초기 파일럿에서 허용할 중단 시간과 복구 목표를 정합니다.
ALB·NAT Gateway·Kubernetes를 습관적으로 추가하지 않고 보안/가용성/비용 요구에 따라 선택합니다.
현 로컬 Compose 파일을 그대로 외부 서버에 올리는 것을 운영 배포로 간주하지 않습니다.

## 9. 작업 원칙

Backend와 Frontend는 별도의 Git 저장소입니다. 각 저장소의 브랜치를 따로 확인합니다.
Git 브랜치 조작, 스테이징, 커밋, 푸시, 머지 및 원격 설정 변경은 사용자가 직접 수행합니다.
에이전트는 요청받은 파일 편집과 검증, 읽기 전용 상태 확인만 수행합니다.

## 공식 참고 문서

- [Spring Boot](https://docs.spring.io/spring-boot/)
- [Docker Compose](https://docs.docker.com/compose/)
- [Flyway](https://documentation.red-gate.com/flyway)
- [GitHub Actions](https://docs.github.com/en/actions)
- [RDS 백업과 복구](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_CommonTasks.BackupRestore.html)
