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

### 4. 영수증 OCR 실행
영수증 OCR은 PaddleOCR 전용 컨테이너를 함께 실행해야 합니다. 처음 기동할 때는 모델 다운로드 때문에 healthcheck가 준비 상태가 되기까지 최대 5분 정도 걸릴 수 있습니다.

```powershell
docker compose --profile app up -d --build
docker compose --profile app ps
```

`ocr`가 `healthy`가 된 뒤 API를 실행합니다. IntelliJ로 API만 실행하는 경우에는 OCR 컨테이너만 별도로 띄울 수 있습니다.

```powershell
docker compose --profile app up -d ocr
```

IntelliJ로 API를 실행할 때 OCR 주소는 로컬 `.env`의 `RECEIPT_OCR_URL=http://localhost:8000`을 사용합니다. Docker Compose로 API까지 함께 실행할 때는 Compose가 컨테이너 내부 주소 `http://ocr:8000`을 자동 사용합니다. `GEMINI_API_KEY`는 선택값이며, 비워 두면 PaddleOCR 원문과 기본 분류만 저장합니다.

### 5. 로컬 S3 테스트 (필요한 경우만)
기본값은 `AWS_S3_ENABLED=false`이며 로컬 파일 저장소를 사용합니다. 실제 S3 업로드를 시험할 팀원만 개인 `.env`에 아래 값을 넣습니다. 실제 키는 공유 저장소나 PR에 올리지 않습니다.

```text
AWS_S3_ENABLED=true
AWS_S3_BUCKET=<팀 S3 버킷 이름>
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=<개인 IAM Access Key ID>
AWS_SECRET_ACCESS_KEY=<개인 IAM Secret Access Key>
```

영수증은 `organizations/{organizationId}/receipts/*`, 프로필 이미지는 `organizations/{organizationId}/profiles/*` 경로에 저장됩니다. 개발 IAM 정책은 두 prefix에 대해서만 `s3:ListBucket`, `s3:GetObject`, `s3:PutObject`, `s3:DeleteObject`를 허용해야 합니다.

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

`.env`의 데이터베이스, JWT, OAuth, S3 값과 선택적인 `GEMINI_API_KEY`를 실제 운영 값으로 변경해야 합니다. EC2에는 장기 AWS Access Key를 저장하지 않고 다음 권한을 가진 IAM Role을 연결합니다. `RECEIPT_OCR_URL`은 운영 Compose 내부 통신 주소인 `http://ocr:8000`을 유지합니다.

운영 프론트는 Vercel의 `/api/*` 리라이트를 통해 API를 같은 출처에서 호출합니다. EC2의 `.env`에서 `AUTH_COOKIE_SECURE=true`와 `AUTH_ALLOWED_REDIRECT_URIS=https://dongbang-frontend.vercel.app,https://dongbang-frontend.vercel.app/auth/callback`을 설정합니다. Google/Kakao 개발자 콘솔과 EC2의 콜백 URI는 각각 `https://dongbang-frontend.vercel.app/api/v1/auth/oauth/google/callback`, `https://dongbang-frontend.vercel.app/api/v1/auth/oauth/kakao/callback`으로 일치시킵니다. GitHub Actions는 운영 `.env`를 덮어쓰지 않으므로 예시 파일 변경만으로는 반영되지 않습니다.

프론트는 로그인 후 `GET /api/v1/auth/csrf`를 `credentials: 'include'`로 호출하여 `result.token`과 `result.headerName`을 받습니다. 이후 POST/PUT/PATCH/DELETE 요청에는 쿠키를 포함하고 `X-XSRF-TOKEN: <result.token>`을 전송합니다. 운영 브라우저에서 로그인부터 변경 요청과 로그아웃까지 확인해야 합니다.

회원 탈퇴는 인증된 사용자가 `DELETE /api/v1/users/me`로 요청합니다. 탈퇴 시 로그인 쿠키와 모든 서버 세션·소셜 계정 연결을 해제하고, 프로필과 연결된 동아리 회원 정보를 익명화합니다. 예정된 행사 신청은 취소해 정원을 반환하며 지난 출석·회비 기록은 보존합니다. 활동 중인 동아리의 회장은 먼저 운영진에게 대표 권한을 위임해야 합니다(`ORG_400_002`). 프론트에서는 이 오류에 위임 안내를 표시하고, 성공 시 보관 중인 사용자·동아리 상태를 비운 뒤 로그인 화면으로 이동해야 합니다.

동아리 탈퇴·강퇴·활동 상태를 휴면(INACTIVE)으로 변경할 때도 예정된 행사 신청을 취소합니다. 이미 시작된 행사의 과거 신청·출석 기록은 유지합니다. 활동으로 다시 변경해도 취소된 신청은 자동 복구되지 않습니다.

OAuth 제공자에서 로그인을 취소하거나 인증 처리에 실패하면, 서명된 `state`의 허용된 프론트 콜백으로 `loginError=cancelled|failed`를 전달합니다. 프론트는 로그인 화면에서 이를 안내합니다. 로그인 성공에는 `state` 쿠키 일치 검증도 적용합니다.

- `s3:PutObject`
- `s3:GetObject`
- `s3:DeleteObject`

객체 권한은 실제 업로드 경로인 `organizations/*/receipts/*`, `organizations/*/photos/*`, `organizations/*/ledger-evidence/*`, `users/*/profile-images/*`로 제한합니다. 애플리케이션은 S3 목록 조회를 사용하지 않으므로 `s3:ListBucket` 권한은 필요하지 않습니다.

GitHub 저장소에는 `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY` Actions Secret이 필요합니다. 운영 S3 버킷은 비공개로 유지합니다. 이미지 URL은 인증된 API 응답에서 설정된 `AWS_S3_URL_TTL` 동안만 유효한 S3 서명 URL로 제공합니다. 기존 EC2 `.env`에 `AWS_S3_CUSTOM_DOMAIN`이 있다면 제거하세요. 이 값은 더 이상 사용하지 않습니다.

기존 CloudFront 배포가 같은 객체를 서명 없는 URL로 제공하고 있다면 애플리케이션 수정만으로 이미 공유된 CDN URL을 차단할 수 없습니다. 해당 배포의 비인증 조회가 거부되는지 확인하고, 필요한 경우 CloudFront 서명 URL/쿠키를 적용하거나 공개 경로를 중단하세요. S3 퍼블릭 액세스 차단과 CloudFront의 S3 원본 접근 제어도 함께 확인해야 합니다.
