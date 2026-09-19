# DongBang Backend

Java 21 · Spring Boot 4.1.1 · PostgreSQL 17 · Docker Compose

로컬 개발, CI, GitHub Actions 기반 EC2 배포를 지원합니다.

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

## 🚀 로컬 개발 환경 셋업

### 1. 환경변수 준비
최초 1회 `.env.example`을 복사하여 `.env` 파일을 생성합니다.
```powershell
Copy-Item .env.example .env   # macOS/Linux: cp .env.example .env
```
> ⚠️ **주의**: `.env` 파일은 실제 민감한 비밀값이 포함될 수 있으므로 **절대 Git에 커밋하지 않습니다** (`.gitignore` 등록됨).

### 2. 로컬 PostgreSQL 데이터베이스 실행
Docker Desktop을 실행한 후 로컬 DB 컨테이너를 구동합니다:
```powershell
docker compose up -d db
```
* DB 포트: `localhost:5432`
* DB 이름: `dongbang`
* 접속 계정: `dongbang` / (비밀번호는 각자의 로컬 `.env` 참조)

### 3. IDE(IntelliJ IDEA) 실행 설정
* 기본 프로필이 `local`로 지정되어 있어, `docker compose up -d db` 실행 후 IntelliJ에서 `DongBangApplication`을 바로 **Run(▶)** 하시면 로컬 DB와 자동 연동되어 부팅됩니다.
* 환경 변수를 직접 지정하고 싶다면 IntelliJ의 **[Edit Configurations] ➔ [Environment variables]**에 로컬 `.env`의 값을 입력합니다:
  ```text
  DB_URL=jdbc:postgresql://localhost:5432/dongbang;DB_USERNAME=dongbang;DB_PASSWORD=<로컬_비밀번호>;PORT=8080
  ```
  *(팁: IntelliJ 플러그인 `EnvFile`을 설치하면 `.env` 파일을 체크 한 번으로 자동 주입할 수 있습니다.)*

---

## 👥 팀 협업 및 Git 컨벤션

### 1. 환경변수 공유 규칙
* **로컬 공통 변수**: `.env.example`에 기본값과 주석을 적어 Git으로 공유합니다.
* **외부 비공개 시크릿 (OAuth Secret, JWT Secret 등)**:
  * Git에 절대 올리지 않으며, **팀 비공개 슬랙/디스코드 채널(Canvas) 또는 팀 1Password 금고**를 통해서만 안전하게 공유합니다.

### 2. 브랜치 명명 규칙
GitHub Issues에 등록된 이슈 번호와 도메인 책임을 명확히 적습니다.
```text
[타입]/#[이슈번호]-[도메인]-[기능요약]
```
* `feat/#10-organization-core` : 신규 기능 개발
* `fix/#15-attendance-qr-error` : 버그 수정
* `chore/#12-infra-env-setup` : 빌드/설정/인프라 작업

### 3. 커밋 메시지 규칙 (Conventional Commits)
```text
타입(도메인): 작업 내용 요약 (#이슈번호)
```
* 예시: `feat(organization): 동아리 CRUD 및 초대·가입, 권한 관리 구현 (#10)`
* 예시: `fix(attendance): 만료된 QR 체크인 검증 오류 수정 (#15)`
* 예시: `chore(infra): 로컬 환경변수 템플릿 및 기본 프로필 설정 (#12)`

### 4. Pull Request(PR) 규칙
* PR 생성 시 등록된 **PR 템플릿**의 체크리스트를 확인하고 작성합니다.
* 제목에 이슈 번호를 표기하고, 본문 상단에 `resolves #이슈번호`를 적어 머지 시 이슈가 자동 Close 되도록 합니다.

---

## 🧪 테스트·빌드

```powershell
.\gradlew.bat test bootJar
```
* DB 연결 없이 빠르게 실행 가능한 단위/슬라이스 테스트 및 JAR 빌드 명령어입니다.

---

## 🔄 CI 파이프라인
* PR 생성 및 `main`/`develop` 푸시 시 GitHub Actions가 자동으로 테스트와 빌드를 검증합니다.

## 🚢 운영 배포

`main` 브랜치에 반영되면 Backend CD 워크플로가 아래 순서로 배포합니다.

1. 단위 테스트와 PostgreSQL 통합 테스트 실행
2. ARM64 운영 이미지를 GHCR에 커밋 SHA 태그로 푸시
3. `compose.prod.yaml`과 `.env.prod.example`을 EC2에 동기화
4. 지정된 SHA 이미지를 배포하고 readiness 상태까지 대기

### 최초 EC2 설정

```bash
bash scripts/ec2-setup.sh
cd /home/ubuntu/dongbang
cp .env.prod.example .env
chmod 600 .env
```

`.env`의 데이터베이스, JWT, OAuth, S3 값을 실제 운영 값으로 변경해야 합니다. EC2에는 장기 AWS Access Key를 저장하지 않고 다음 권한을 가진 IAM Role을 연결합니다.

- `s3:PutObject`
- `s3:GetObject`
- `s3:DeleteObject`

GitHub 저장소에는 `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY` Actions Secret이 필요합니다. 운영 S3 버킷은 비공개로 유지하고, 공개 조회가 필요하면 `AWS_S3_CUSTOM_DOMAIN`에 CloudFront 도메인을 설정합니다.
