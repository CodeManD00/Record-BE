# Record Project

공연 티켓 기록 및 리뷰 관리 시스템 - Backend 프로젝트

이 프로젝트는 **Backend (Spring Boot)** 기반 RESTful API 서버입니다.

## Git Clone 후 재생성 가능

이 프로젝트는 `git clone` 후 README를 보고 바로 재생성할 수 있도록 모든 필수 요소를 포함하고 있습니다.

### Git Clone 후 재생성 가능 확인

#### 1. Code를 만들어 낼 수 있는 방법 및 Script 포함

**Makefile 및 빌드 스크립트 포함:**
- 프로젝트 루트: `Record-BE/Makefile` - 통합 Makefile
- Backend: `Record-BE/Record-BE/build.gradle` - Gradle 빌드 스크립트

**코드 생성 방법:**
```bash
# Backend 빌드
make be-build
```

자세한 내용: [How to Build](#how-to-build), [Scripts - 통합 Makefile](#통합-makefile)

#### 2. Proto-system 설치 방법 및 Script 포함

**Proto-system 설치 스크립트:**
- 위치: `Record-BE/Record-BE/setup.sh`
- 기능: 전체 시스템 자동 설치 (사전 요구사항 확인, 환경 변수 설정, DB 생성, 빌드)

**사용 방법:**
```bash
cd Record-BE/Record-BE
chmod +x setup.sh
./setup.sh
```

자세한 내용: [Scripts - Proto-system 설치 스크립트](#proto-system-설치-스크립트), [How to Install](#how-to-install)

#### 3. Sample/Proto-data 포함

**포함된 데이터:**
- 데이터베이스 스키마: `Record-BE/Record-BE/SQL for local DB`
- 샘플 데이터 생성 스크립트: `Record-BE/Record-BE/generate-sample-data.sh`
- 생성되는 샘플 데이터:
  - 사용자 3명 (관리자 1명, 일반 사용자 2명)
  - 뮤지컬 정보 3개
  - 티켓 데이터 3개
  - 리뷰 데이터 2개
  - 친구 관계 1개

**사용 방법:**
```bash
cd Record-BE/Record-BE
chmod +x generate-sample-data.sh
./generate-sample-data.sh
```

자세한 내용: [Description of Sample Data](#description-of-sample-data), [Database or Data Used](#database-or-data-used)

#### 4. 연구 트랙 실험 데이터/결과물

이 프로젝트는 **개발 트랙** 프로젝트이므로 실험 데이터/결과물은 포함되지 않습니다.

#### 5. Open Source 사용 내용 포함

**모든 사용 오픈소스 라이선스 정보 포함:**
- Backend 오픈소스 목록 및 라이선스
- 각 라이브러리의 버전 및 라이선스 링크

자세한 내용: [Description of Used Open Source](#description-of-used-open-source)

## 목차

### 필수 항목 (README 요구사항)

1. [Source Code 설명](#source-code-설명)
2. [How to Build](#how-to-build)
3. [How to Install](#how-to-install)
4. [How to Test](#how-to-test)
5. [Description of Sample Data](#description-of-sample-data)
6. [Database or Data Used](#database-or-data-used)
7. [Description of Used Open Source](#description-of-used-open-source)

### 추가 정보

- [프로젝트 개요](#프로젝트-개요)
- [프로젝트 구조](#프로젝트-구조)
- [빠른 시작](#빠른-시작)
- [환경 변수 및 API 키 설정](#환경-변수-및-api-키-설정)
- [Backend (Record-BE)](#backend-record-be)
- [Scripts](#scripts)

## Source Code 설명

### 프로젝트 개요

Record는 공연 티켓을 디지털로 기록하고, 공연 리뷰를 작성하며, AI 기반 기능을 활용하여 공연 경험을 향상시키는 애플리케이션의 Backend 서버입니다.

이 프로젝트는 **Backend (Spring Boot)** 기반 RESTful API 서버로, 클라이언트 애플리케이션에 데이터와 기능을 제공합니다.

### 시스템 구성

#### Backend (Record-BE)
- **언어**: Java 21
- **프레임워크**: Spring Boot 3.2.5
- **아키텍처**: 계층형 아키텍처 (Controller-Service-Repository)
- **데이터베이스**: PostgreSQL
- **인증**: JWT 기반 인증
- **API 문서**: Swagger/OpenAPI
- **빌드 도구**: Gradle
- **주요 패키지 구조**:
  - `auth/` - 인증 관련 (JWT, 이메일 인증, 소셜 로그인)
  - `user/` - 사용자 관리
  - `review/` - 리뷰 관리
  - `ocr/` - OCR 기능 (Google Cloud Vision)
  - `STTorText/` - STT 기능 (OpenAI Whisper)
  - `AWS/` - AWS S3 파일 저장
  - `config/` - 설정 클래스

### 코드 생성 방법

이 프로젝트의 코드는 다음 방법으로 생성할 수 있습니다:

1. **Makefile 사용** (권장)
   ```bash
   make be-build    # Backend 빌드
   ```

2. **Gradle 직접 사용**
   ```bash
   ./gradlew build
   ```

자세한 내용은 [How to Build](#how-to-build) 섹션을 참고하세요.

### 주요 기능

- **사용자 인증**: JWT 기반 인증, 이메일 인증, 소셜 로그인
- **티켓 관리**: 티켓 추가, 조회, 수정, 삭제
- **OCR 기능**: 티켓 이미지에서 텍스트 자동 추출
- **STT 기능**: 음성 녹음 및 텍스트 변환
- **AI 이미지 생성**: DALL-E를 활용한 이미지 생성
- **리뷰 작성**: 템플릿 기반 리뷰 작성 및 관리
- **캘린더**: 공연 일정 캘린더 뷰
- **친구 관리**: 친구 추가 및 친구 목록 조회
- **통계 및 분석**: 연도별 요약, 통계 정보

## 빠른 시작

프로젝트를 빠르게 시작하려면 다음 중 하나를 선택하세요:

**자동 설치 (권장):**
```bash
cd Record-BE/Record-BE
chmod +x setup.sh
./setup.sh
```

**수동 설치:**
자세한 설치 방법은 [How to Install](#how-to-install) 섹션을 참고하세요.

**프로젝트 구조:**
자세한 프로젝트 구조는 [추가 정보 - 프로젝트 구조](#프로젝트-구조) 섹션을 참고하세요.

## 환경 변수 및 API 키 설정

프로젝트를 처음 설정할 때 필요한 모든 환경 변수와 API 키 설정 가이드입니다.

### 빠른 참조

**필수 설정 (반드시 필요):**
- PostgreSQL 데이터베이스 연결 정보
- JWT 시크릿 키

**선택적 설정 (기능별):**
- OCR 기능 → Google Cloud Vision API
- STT 기능 → OpenAI API
- AI 이미지 생성 → OpenAI DALL-E API
- 파일 저장 → AWS S3
- 이메일 인증 → Gmail 앱 비밀번호
- Google Sign-In → Google OAuth 2.0

**최소 설정으로 시작 가능:** 데이터베이스 + JWT 시크릿 키만으로도 기본 기능 사용 가능

### 필수 설정

다음 설정은 **반드시** 필요합니다:

#### 1. PostgreSQL 데이터베이스 연결 정보

**환경 변수명:** `DB_URL`, `DB_USER`, `DB_PASSWORD`

**설명:**
- `DB_URL`: PostgreSQL 데이터베이스 연결 URL
- `DB_USER`: 데이터베이스 접속 사용자명
- `DB_PASSWORD`: 데이터베이스 접속 비밀번호

**설정 방법:**

1. **PostgreSQL 설치 및 실행 확인**
   ```bash
   # PostgreSQL 실행 확인
   psql --version
   
   # PostgreSQL 서비스 시작 (macOS)
   brew services start postgresql
   
   # PostgreSQL 서비스 시작 (Linux)
   sudo systemctl start postgresql
   ```

2. **데이터베이스 및 사용자 생성**
   ```bash
   # PostgreSQL에 접속 (postgres 사용자로)
   psql -U postgres
   
   # 데이터베이스 생성
   CREATE DATABASE recorddb;
   
   # 사용자 생성 (원하는 사용자명과 비밀번호로 변경)
   CREATE USER your_db_user WITH PASSWORD 'your_secure_password';
   
   # 권한 부여
   GRANT ALL PRIVILEGES ON DATABASE recorddb TO your_db_user;
   \q
   ```

3. **환경 변수 설정**
   ```bash
   # .env 파일에 추가하거나 환경 변수로 설정
   export DB_URL=jdbc:postgresql://localhost:5432/recorddb
   export DB_USER=your_db_user
   export DB_PASSWORD=your_secure_password
   ```

**보안 주의사항:**
- **절대** `recordpass` 같은 기본 비밀번호를 사용하지 마세요
- 강력한 비밀번호를 사용하세요 (최소 12자 이상, 대소문자, 숫자, 특수문자 포함)
- `.env` 파일은 절대 Git에 커밋하지 마세요
- 프로덕션 환경에서는 환경 변수 관리 도구를 사용하세요

#### 2. JWT 시크릿 키

**환경 변수명:** `JWT_SECRET`

**설명:**
- JWT(JSON Web Token) 토큰을 서명하고 검증하는 데 사용되는 비밀 키
- 인증 토큰의 보안을 보장하기 위해 반드시 설정해야 함
- 최소 32바이트 이상의 랜덤 문자열이 필요함

**설정 방법:**

1. **시크릿 키 생성**
   ```bash
   # 방법 1: OpenSSL 사용 (권장)
   openssl rand -base64 32
   
   # 방법 2: Python 사용
   python3 -c "import secrets; print(secrets.token_urlsafe(32))"
   
   # 방법 3: Node.js 사용
   node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
   ```

2. **환경 변수 설정**
   ```bash
   # 생성된 키를 환경 변수로 설정
   export JWT_SECRET=생성된_랜덤_문자열
   ```

**보안 주의사항:**
- **절대** 예시 값(`your-secret-key-minimum-32-bytes-long`)을 그대로 사용하지 마세요
- 각 환경(개발, 스테이징, 프로덕션)마다 다른 키를 사용하세요
- 키를 안전하게 보관하고 정기적으로 변경하세요

### 선택적 설정 (기능별)

다음 설정은 해당 기능을 사용할 때만 필요합니다:

#### OCR 기능 (티켓 이미지 텍스트 추출)

**환경 변수명:** `GOOGLE_APPLICATION_CREDENTIALS`

**설명:**
- Google Cloud Vision API를 사용하여 티켓 이미지에서 텍스트를 자동으로 추출하는 기능
- 서비스 계정의 JSON 키 파일 경로를 설정해야 함

**설정 방법:**

1. **Google Cloud 프로젝트 생성**
   - [Google Cloud Console](https://console.cloud.google.com/) 접속
   - 새 프로젝트 생성 또는 기존 프로젝트 선택

2. **Vision API 활성화**
   - API 및 서비스 → 라이브러리로 이동
   - "Cloud Vision API" 검색
   - "사용 설정" 클릭

3. **서비스 계정 생성**
   - IAM 및 관리자 → 서비스 계정으로 이동
   - "서비스 계정 만들기" 클릭
   - 서비스 계정 이름 입력 (예: `record-vision-api`)
   - 역할: "Cloud Vision API 사용자" 선택
   - "완료" 클릭

4. **JSON 키 파일 다운로드**
   - 생성된 서비스 계정 클릭
   - "키" 탭 → "키 추가" → "JSON" 선택
   - 다운로드된 JSON 파일을 안전한 위치에 저장 (예: `~/.config/record-vision-key.json`)

5. **환경 변수 설정**
   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/your-service-account-key.json
   ```

**비용:** 월 1,000회 무료, 이후 $1.50 per 1,000 units

#### STT 기능 (음성 텍스트 변환)

**환경 변수명:** `OPENAI_API_KEY`

**설명:**
- OpenAI Whisper API를 사용하여 음성 녹음을 텍스트로 변환하는 기능
- 사용자가 음성으로 리뷰를 작성할 수 있도록 지원

**설정 방법:**

1. **OpenAI 계정 생성 및 API 키 발급**
   - [OpenAI Platform](https://platform.openai.com) 접속
   - 계정 생성 또는 로그인
   - API Keys 메뉴로 이동
   - "Create new secret key" 클릭
   - 생성된 키를 안전하게 복사 (한 번만 표시됨)

2. **환경 변수 설정**
   ```bash
   export OPENAI_API_KEY=sk-프로젝트에서_발급받은_실제_API_키
   ```

**비용:** $0.006 per minute

#### AI 이미지 생성

**환경 변수명:** `OPENAI_API_KEY` (STT와 동일)

**설명:**
- OpenAI DALL-E API를 사용하여 리뷰 내용을 기반으로 이미지를 생성하는 기능
- STT 기능과 동일한 OpenAI API 키를 사용

**설정 방법:**
- STT 기능 설정과 동일 (위 참고)

**비용:** $0.040 per image

#### 파일 저장 (프로필 이미지, 생성된 이미지)

**환경 변수명:** `RecAWS_ACCESS_KEY_ID`, `RecAWS_SECRET_ACCESS_KEY`, `RecS3_BUCKET`, `AWS_REGION`

**설명:**
- AWS S3를 사용하여 사용자 프로필 이미지와 AI로 생성된 이미지를 저장하는 기능
- IAM 사용자의 Access Key와 Secret Access Key가 필요함

**설정 방법:**

1. **AWS 계정 생성**
   - [AWS Console](https://aws.amazon.com/console/) 접속
   - 계정 생성 또는 로그인

2. **IAM 사용자 생성**
   - IAM 서비스로 이동
   - "사용자" → "사용자 추가" 클릭
   - 사용자 이름 입력 (예: `record-s3-user`)
   - "프로그래밍 방식 액세스" 선택
   - 권한: "기존 정책 직접 연결" → `AmazonS3FullAccess` 선택
   - Access Key ID와 Secret Access Key를 안전하게 저장

3. **S3 버킷 생성**
   - S3 서비스로 이동
   - "버킷 만들기" 클릭
   - 버킷 이름 입력 (예: `record-app-images`)
   - 리전 선택 (예: `ap-northeast-2` - 서울)
   - 버킷 생성 완료

4. **환경 변수 설정**
   ```bash
   export RecAWS_ACCESS_KEY_ID=AKIA실제_발급받은_Access_Key_ID
   export RecAWS_SECRET_ACCESS_KEY=실제_발급받은_Secret_Access_Key
   export RecS3_BUCKET=생성한_버킷_이름
   export AWS_REGION=ap-northeast-2
   ```

**비용:** S3 스토리지 및 요청 비용 (무료 티어: 5GB 스토리지, 20,000 GET 요청/월)

#### 이메일 인증

**환경 변수명:** `MAIL_USERNAME`, `MAIL_PASSWORD`

**설명:**
- 사용자 이메일 인증 기능을 위한 SMTP 서버 설정
- Gmail의 앱 비밀번호를 사용하거나 다른 SMTP 서버 사용 가능

**설정 방법 (Gmail 사용 시):**

1. **Google 계정 2단계 인증 활성화**
   - Google 계정 → 보안으로 이동
   - "2단계 인증" 활성화 (필수)

2. **앱 비밀번호 생성**
   - Google 계정 → 보안 → 2단계 인증 → 앱 비밀번호
   - "앱 선택" → "메일" 선택
   - "기기 선택" → "기타(맞춤 이름)" 선택
   - 이름 입력 (예: `Record App`)
   - 생성된 16자리 비밀번호 복사

3. **환경 변수 설정**
   ```bash
   export MAIL_USERNAME=your-email@gmail.com
   export MAIL_PASSWORD=생성된_16자리_앱_비밀번호
   ```

**보안 주의사항:**
- 일반 Gmail 비밀번호를 사용하지 마세요 (작동하지 않음)
- 반드시 앱 비밀번호를 사용하세요
- 앱 비밀번호는 안전하게 보관하세요

### 환경 변수 설정 파일 예시

**Backend `.env` 파일 생성:**

프로젝트 루트에 `.env` 파일을 생성하여 환경 변수를 관리하는 것을 권장합니다:

```bash
# Record-BE/Record-BE/.env 파일 생성
cd Record-BE/Record-BE
cat > .env << 'EOF'
# ============================================
# 필수 설정 (반드시 설정 필요)
# ============================================

# PostgreSQL 데이터베이스 연결 정보
# DB_URL: 데이터베이스 연결 URL (형식: jdbc:postgresql://호스트:포트/데이터베이스명)
DB_URL=jdbc:postgresql://localhost:5432/recorddb

# DB_USER: 데이터베이스 접속 사용자명 (위에서 생성한 사용자명)
DB_USER=your_db_user

# DB_PASSWORD: 데이터베이스 접속 비밀번호 (위에서 설정한 비밀번호)
# 보안: 강력한 비밀번호를 사용하고 절대 Git에 커밋하지 마세요
DB_PASSWORD=your_secure_password_here

# JWT 시크릿 키 (최소 32바이트 이상의 랜덤 문자열)
# 생성 방법: openssl rand -base64 32
# 보안: 각 환경마다 다른 키를 사용하고 절대 Git에 커밋하지 마세요
JWT_SECRET=your_generated_jwt_secret_key_here

# ============================================
# 선택적 설정 (기능 사용 시에만 필요)
# ============================================

# OpenAI API (STT, 이미지 생성, 리뷰 정리 기능)
# 발급: https://platform.openai.com
# 보안: 실제 API 키로 교체하고 절대 Git에 커밋하지 마세요
OPENAI_API_KEY=sk-your_actual_openai_api_key_here

# AWS S3 (파일 저장 기능)
# 발급: https://aws.amazon.com/console/
# 보안: 실제 키로 교체하고 절대 Git에 커밋하지 마세요
RecAWS_ACCESS_KEY_ID=AKIAyour_actual_access_key_id_here
RecAWS_SECRET_ACCESS_KEY=your_actual_secret_access_key_here
RecS3_BUCKET=your-bucket-name
AWS_REGION=ap-northeast-2

# Google Cloud Vision API (OCR 기능)
# 발급: https://console.cloud.google.com/
# JSON 키 파일의 전체 경로를 입력하세요
GOOGLE_APPLICATION_CREDENTIALS=/path/to/your/service-account-key.json

# 이메일 설정 (이메일 인증 기능)
# Gmail 앱 비밀번호 사용 (2단계 인증 필수)
# 보안: 실제 이메일과 앱 비밀번호로 교체하고 절대 Git에 커밋하지 마세요
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your_16_digit_app_password_here
EOF
```

**환경 변수 로드:**

`.env` 파일을 생성한 후, 환경 변수를 로드하려면:

```bash
# 방법 1: source 명령어 사용
source .env

# 방법 2: 실행 시 자동 로드 (Backend)
set -a
source .env
set +a
make run
```

**보안 주의사항:**
- `.env` 파일은 `.gitignore`에 포함되어 있어야 합니다
- **절대** Git에 커밋하지 마세요
- 모든 예시 값(`recordpass`, `your-secret-key` 등)을 실제 값으로 교체하세요
- 프로덕션 환경에서는 환경 변수 관리 도구(AWS Secrets Manager, HashiCorp Vault 등)를 사용하세요

### 설정 확인

환경 변수가 올바르게 설정되었는지 확인:

```bash
# Backend
cd Record-BE/Record-BE
echo $DB_URL
echo $JWT_SECRET
echo $OPENAI_API_KEY
```

## Backend (Record-BE)

### 개요

Spring Boot 기반 RESTful API 서버로, 클라이언트 애플리케이션에 데이터와 기능을 제공합니다.

### 기술 스택

- **Java 21**
- **Spring Boot 3.2.5**
- **PostgreSQL**
- **JWT (JJWT)**
- **AWS S3**
- **Google Cloud Vision API** (OCR)
- **OpenAI API** (Whisper, GPT, DALL-E)

### 주요 기능

#### 1. 사용자 인증 및 관리
- JWT 기반 인증
- 이메일 인증
- 비밀번호 관리
- 프로필 관리

#### 2. 티켓 관리
- 티켓 CRUD
- 티켓 검색 및 필터링
- 티켓 통계

#### 3. 리뷰 관리
- 리뷰 작성 및 관리
- 리뷰 질문 관리
- 리뷰 좋아요

#### 4. OCR 기능
- Google Cloud Vision API를 사용한 이미지 텍스트 추출
- 공연 정보 자동 파싱

#### 5. STT 기능
- OpenAI Whisper API를 사용한 음성 텍스트 변환

#### 6. AI 기반 기능
- GPT를 사용한 리뷰 정리 및 요약
- DALL-E를 사용한 이미지 생성

#### 7. 파일 저장
- AWS S3를 통한 이미지 파일 저장

#### 8. 친구 관리
- 친구 요청 및 수락
- 친구 목록 조회

### 아키텍처

```
┌─────────────────────────────────────┐
│      Controller Layer               │  ← REST API 엔드포인트
│  (AuthController, TicketController) │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Service Layer                   │  ← 비즈니스 로직
│  (UserService, TicketService, etc.)  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Repository Layer                │  ← 데이터 접근
│  (UserRepository, TicketRepository)  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Database (PostgreSQL)           │  ← 데이터 저장
└─────────────────────────────────────┘
```


## 시스템 아키텍처

```
│              │ Services  │                            │
│              │  (API)    │                            │
│              └─────┬─────┘                            │
└────────────────────┼───────────────────────────────────┘
                    │ HTTP/REST API
                    │ (JWT Authentication)
┌───────────────────▼───────────────────────────────────┐
│              Backend Server (Spring Boot)             │
│                    Record-BE                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐           │
│  │Controller│  │ Service  │  │Repository│           │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘           │
│       │             │             │                  │
│       └─────────────┴─────────────┘                  │
│                    │                                  │
│              ┌─────▼─────┐                           │
│              │PostgreSQL │                           │
│              │ Database  │                           │
│              └───────────┘                           │
└───────────────────────────────────────────────────────┘
                    │
        ┌───────────┼───────────┐
        │           │           │
┌───────▼───┐ ┌─────▼─────┐ ┌──▼────────┐
│  AWS S3   │ │  OpenAI   │ │  Google   │
│ (Images)  │ │   API     │ │   Cloud   │
└───────────┘ └───────────┘ └───────────┘
```

## How to Build

**Code를 만들어 낼 수 있는 방법 및 Script (Makefile 포함)**

이 섹션에서는 프로젝트의 코드를 생성하고 빌드하는 모든 방법을 설명합니다. Makefile을 포함한 모든 빌드 스크립트가 포함되어 있습니다.

### Backend 빌드

**Backend 빌드:**
```bash
cd Record-BE/Record-BE
make build
```

또는 프로젝트 루트에서:

```bash
make be-build
```

### Backend 빌드

#### 사전 요구사항
- Java 21 이상
- Gradle 8.x 이상 (또는 Gradle Wrapper 사용)
- PostgreSQL 12 이상

#### 빌드 방법

```bash
cd Record-BE/Record-BE
./gradlew build
```

또는 Makefile 사용:
```bash
make build
```

## How to Install

**Proto-system 설치 방법 및 Script**

이 섹션에서는 프로젝트를 처음부터 설치하는 방법을 설명합니다. 자동 설치 스크립트(`setup.sh`)와 수동 설치 방법을 모두 포함합니다.

### Backend 설치 및 실행

#### 1. 저장소 클론

```bash
git clone <repository-url>
cd Record-BE
```

#### 2. 환경 변수 설정 및 데이터베이스 설정

```bash
cd Record-BE/Record-BE

# 환경 변수 설정 (실제 값으로 교체 필요)
# 보안: 아래 예시 값들을 실제 값으로 반드시 교체하세요
export DB_URL=jdbc:postgresql://localhost:5432/recorddb
export DB_USER=your_db_user
export DB_PASSWORD=your_secure_password
export JWT_SECRET=$(openssl rand -base64 32)

# 데이터베이스 설정
psql -U postgres
CREATE DATABASE recorddb;
CREATE USER your_db_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE recorddb TO your_db_user;
\q

# 의존성 설치 및 실행
make install
make run
```

또는 프로젝트 루트에서:

```bash
# 환경 변수 설정 (실제 값으로 교체 필요)
# 보안: 아래 예시 값들을 실제 값으로 반드시 교체하세요
export DB_URL=jdbc:postgresql://localhost:5432/recorddb
export DB_USER=your_db_user
export DB_PASSWORD=your_secure_password
export JWT_SECRET=$(openssl rand -base64 32)

# 통합 Makefile 사용
make be-install
make be-run
```

**보안 주의사항:**
- `your_db_user`, `your_secure_password` 등을 실제 값으로 반드시 교체하세요
- 강력한 비밀번호를 사용하세요 (최소 12자 이상)
- JWT 시크릿 키는 `openssl rand -base64 32` 명령어로 생성하세요

Backend는 http://localhost:8080 에서 실행됩니다.

### Backend 설치 및 실행

#### 1. 환경 변수 설정

**필수 환경 변수:**
```bash
# 보안: 아래 예시 값들을 실제 값으로 반드시 교체하세요
export DB_URL=jdbc:postgresql://localhost:5432/recorddb
export DB_USER=your_db_user
export DB_PASSWORD=your_secure_password
export JWT_SECRET=$(openssl rand -base64 32)
```

**중요:** `your_db_user`, `your_secure_password` 등을 실제 값으로 반드시 교체하세요. 자세한 설정 방법은 [환경 변수 및 API 키 설정](#환경-변수-및-api-키-설정) 섹션을 참고하세요.

**선택적 환경 변수 (기능 사용 시):**
- OpenAI API: STT, 이미지 생성, 리뷰 정리 기능
- AWS S3: 파일 저장 기능
- Google Cloud Vision API: OCR 기능
- 이메일 설정: 이메일 인증 기능

자세한 환경 변수 설정 방법은 [환경 변수 및 API 키 설정](#환경-변수-및-api-키-설정) 섹션을 참고하세요.

#### 2. 데이터베이스 설정

```bash
# PostgreSQL 접속
psql -U postgres

# 데이터베이스 생성
CREATE DATABASE recorddb;

# 사용자 생성 및 권한 부여
# 보안: 'your_secure_password'를 강력한 비밀번호로 교체하세요
CREATE USER your_db_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE recorddb TO your_db_user;
\q
```

**보안 주의사항:**
- `your_db_user`와 `your_secure_password`를 실제 값으로 교체하세요
- 강력한 비밀번호를 사용하세요 (최소 12자 이상, 대소문자, 숫자, 특수문자 포함)

#### 3. 애플리케이션 실행

```bash
cd Record-BE/Record-BE
make run
```

또는 프로젝트 루트에서:

```bash
make be-run
```

또는:

```bash
./gradlew bootRun
```

#### 4. 설치 확인

- **애플리케이션**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html

## How to Test

### Backend 테스트

```bash
cd Record-BE/Record-BE
make test
```

또는 프로젝트 루트에서:

```bash
make be-test
```

또는:

```bash
./gradlew test
```

## Description of Sample Data

**Sample/Proto-data 포함**

프로젝트에는 샘플 데이터 생성 스크립트가 포함되어 있어 개발 및 테스트를 위한 데이터를 자동으로 생성할 수 있습니다.

### 샘플 데이터 생성 스크립트

**위치:** `Record-BE/Record-BE/generate-sample-data.sh`

**기능:**
- 사용자 데이터 자동 생성
- 뮤지컬 정보 자동 생성
- 티켓 데이터 자동 생성
- 리뷰 데이터 자동 생성
- 친구 관계 데이터 자동 생성

**사용 방법:**
```bash
cd Record-BE/Record-BE
chmod +x generate-sample-data.sh
./generate-sample-data.sh
```

**생성되는 데이터:**

1. **사용자 3명**
   - 관리자 1명: `admin@example.com`
   - 일반 사용자 2명: `user1@example.com`, `user2@example.com`
   - **보안 주의:** 샘플 데이터의 비밀번호는 개발/테스트 환경에서만 사용하세요. 프로덕션 환경에서는 반드시 변경하세요.

2. **뮤지컬 정보 3개**
   - 레미제라블
   - 시카고
   - 위키드

3. **티켓 데이터 3개**
   - 사용자별 티켓 정보 포함

4. **리뷰 데이터 2개**
   - 티켓별 리뷰 정보 포함

5. **친구 관계 1개**
   - 사용자 간 친구 관계

**스크립트 내용:**

```bash
#!/bin/bash
# 샘플 데이터 생성 스크립트
# 개발 및 테스트를 위한 샘플 데이터를 생성합니다.

# 환경 변수 로드
source .env

# PostgreSQL에 샘플 데이터 삽입
psql -U $DB_USER -d $DB_NAME << EOF
-- 사용자 데이터
INSERT INTO users (email, password, nickname, role) VALUES
('admin@example.com', 'encoded_password', '관리자', 'ADMIN'),
('user1@example.com', 'encoded_password', '사용자1', 'USER'),
('user2@example.com', 'encoded_password', '사용자2', 'USER')
ON CONFLICT (email) DO NOTHING;

-- 뮤지컬 데이터
INSERT INTO musical_db (title, summary) VALUES
('레미제라블', '빅토르 위고의 소설을 원작으로 한 뮤지컬'),
('시카고', '1920년대 시카고를 배경으로 한 범죄 뮤지컬'),
('위키드', '오즈의 마법사를 다른 관점에서 본 뮤지컬')
ON CONFLICT DO NOTHING;

-- 티켓 및 리뷰 데이터
-- ... (생략)
EOF
```

### 1. API를 통한 데이터 생성

**Swagger UI 사용:**
1. Backend 서버 실행: http://localhost:8080
2. Swagger UI 접속: http://localhost:8080/swagger-ui.html
3. 다음 엔드포인트로 샘플 데이터 생성:
   - `POST /auth/signup` - 사용자 생성
   - `POST /api/tickets` - 티켓 생성
   - `POST /api/reviews` - 리뷰 생성

### 2. SQL 스크립트 사용

**데이터베이스 스키마 생성:**

`Record-BE/Record-BE/SQL for local DB` 파일을 사용하여 데이터베이스 스키마를 생성할 수 있습니다:

```bash
# PostgreSQL 접속하여 실행
psql -U recorduser -d recorddb -f "Record-BE/Record-BE/SQL for local DB"
```

**이 SQL 파일이 생성하는 테이블:**
- `users` - 사용자 정보
- `performances` - 공연 정보
- `tickets` - 티켓 정보
- `reviews` - 리뷰 정보
- `questions_templates` - 질문 템플릿
- `review_questions` - 리뷰 질문
- `generated_image_url` - 생성된 이미지
- `musical_db` - 뮤지컬 정보
- `musical_characters` - 뮤지컬 캐릭터
- `band_db` - 밴드 정보

**주의사항:**
- JPA의 `ddl-auto: update` 설정으로 자동 생성되므로, SQL 스크립트는 선택사항입니다.
- 수동으로 스키마를 관리하려면 SQL 스크립트를 사용하세요.

## Database or Data Used

### 데이터베이스

- **PostgreSQL 12 이상** (권장: PostgreSQL 14+)
- 기본 포트: `5432`
- 기본 데이터베이스명: `recorddb`

### 주요 테이블

- `users` - 사용자 정보
- `tickets` - 티켓 정보
- `reviews` - 리뷰 정보
- `friendships` - 친구 관계
- `email_verifications` - 이메일 인증 정보
- `musical_db` - 뮤지컬 정보
- `band_db` - 밴드 정보
- `musical_character` - 뮤지컬 캐릭터 정보

자세한 스키마 정보는 [Backend README의 Database 섹션](#database-or-data-used)을 참고하세요.

### 외부 데이터 소스

- **AWS S3**: 프로필 이미지 및 생성된 이미지 저장
- **Google Cloud Vision API**: OCR 기능
- **OpenAI API**: 
  - Whisper API: 음성 텍스트 변환
  - GPT API: 텍스트 생성 및 처리
  - DALL-E API: 이미지 생성

## Description of Used Open Source

**Open Source 사용 내용 포함**

### 오픈소스란?

**오픈소스(Open Source)**는 소스 코드가 공개되어 있어 누구나 자유롭게 사용, 수정, 배포할 수 있는 소프트웨어입니다. 이 프로젝트는 개발 효율성과 기능 구현을 위해 많은 오픈소스 라이브러리와 프레임워크를 활용하고 있습니다.

**오픈소스 사용의 장점:**
- 빠른 개발: 이미 검증된 라이브러리를 사용하여 개발 시간 단축
- 안정성: 많은 개발자들이 사용하고 개선한 검증된 코드
- 기능 확장: 필요한 기능을 제공하는 라이브러리 활용
- 커뮤니티 지원: 활발한 커뮤니티와 문서화

이 프로젝트는 다음과 같은 오픈소스 라이브러리와 프레임워크를 사용합니다. 모든 오픈소스의 라이선스 정보가 포함되어 있습니다.

### Backend 오픈소스

#### 핵심 프레임워크
- **Spring Boot** (3.2.5) - [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
  - 웹 애플리케이션 프레임워크
  - 의존성: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-mail`, `spring-boot-starter-webflux`

#### 데이터베이스
- **PostgreSQL JDBC Driver** (42.6.0) - [BSD License](https://jdbc.postgresql.org/about/license.html)
  - PostgreSQL 데이터베이스 연결 드라이버

#### API 문서화
- **SpringDoc OpenAPI** (2.5.0) - [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
  - Swagger UI를 통한 API 문서 자동 생성

#### 인증 및 보안
- **JJWT** (0.11.5) - [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
  - JWT 토큰 생성 및 검증
  - 의존성: `jjwt-api`, `jjwt-impl`, `jjwt-jackson`

#### 클라우드 서비스 SDK
- **AWS SDK for Java v2** (2.25.11) - [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
  - AWS S3 파일 저장 서비스 통합
  - 의존성: `aws-sdk-s3`, `aws-sdk-sts`

- **Google Cloud Vision** (3.36.0) - [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
  - OCR (Optical Character Recognition) 기능

- **Google Cloud Speech** (4.2.0) - [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
  - 음성 인식 기능 (선택적 사용)

#### 개발 도구
- **Lombok** - [MIT License](https://projectlombok.org/license.html)
  - 보일러플레이트 코드 자동 생성 (Getter, Setter, Builder 등)

#### 테스트 프레임워크
- **JUnit 5** - [Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/)
  - 단위 테스트 및 통합 테스트

### 오픈소스 라이선스 요약

이 프로젝트에서 사용하는 모든 오픈소스 라이브러리는 다음과 같은 라이선스를 따릅니다:

- **Apache License 2.0**: Spring Boot, AWS SDK, Google Cloud SDK 등
- **BSD License**: PostgreSQL JDBC Driver
- **Eclipse Public License 2.0**: JUnit 5
- **MIT License**: Lombok

모든 오픈소스 라이브러리는 각각의 라이선스를 준수하며, 상업적 사용이 가능합니다.

**라이선스 호환성:**
- 모든 라이선스는 상업적 사용을 허용합니다.
- Apache License 2.0과 MIT License는 매우 호환성이 높습니다.
- 이 프로젝트는 모든 오픈소스 라이선스를 준수합니다.

## Scripts

**Code 생성 및 Proto-system 설치 Script**

이 프로젝트는 코드를 생성하고 시스템을 설치하기 위한 여러 스크립트를 포함하고 있습니다.

### Proto-system 설치 스크립트

**Proto-system 설치 방법 및 Script**

프로젝트를 처음부터 설정하고 실행할 수 있는 자동화된 설치 스크립트입니다.

#### `setup.sh` - 전체 시스템 자동 설치

**위치:** `Record-BE/Record-BE/setup.sh`

**기능:**
- 사전 요구사항 확인 (Java, PostgreSQL)
- 환경 변수 자동 설정 (.env 파일 생성)
- 데이터베이스 자동 생성 및 설정
- Backend 의존성 자동 설치
- Backend 자동 빌드
- 데이터베이스 스키마 자동 초기화

**사용 방법:**
```bash
cd Record-BE/Record-BE
chmod +x setup.sh
./setup.sh
```

**실행 과정:**
1. 사전 요구사항 확인 (Java 21, PostgreSQL 등)
2. 환경 변수 설정 (.env 파일 생성)
3. 데이터베이스 생성 및 사용자 설정
4. Backend 의존성 다운로드
5. Backend 빌드
6. 데이터베이스 스키마 초기화

**주의사항:**
- PostgreSQL이 실행 중이어야 합니다
- PostgreSQL root 사용자 비밀번호가 필요할 수 있습니다 (기본값: postgres)

### 샘플 데이터 생성 스크립트

개발 및 테스트를 위한 샘플 데이터를 자동으로 생성하는 스크립트입니다.

#### `generate-sample-data.sh` - 샘플 데이터 생성

**위치:** `Record-BE/Record-BE/generate-sample-data.sh`

**생성되는 데이터:**
- 사용자 3명 (관리자 1명, 일반 사용자 2명)
- 뮤지컬 정보 3개
- 티켓 데이터 3개
- 리뷰 데이터 2개
- 친구 관계 1개

**사용 방법:**
```bash
cd Record-BE/Record-BE
chmod +x generate-sample-data.sh
./generate-sample-data.sh
```

**테스트 계정 정보:**
- 이메일: `user1@example.com`
- 이메일: `user2@example.com`
- 이메일: `admin@example.com`

**보안 주의사항:**
- 샘플 데이터의 비밀번호는 개발/테스트 환경에서만 사용하세요
- 프로덕션 환경에서는 반드시 모든 계정의 비밀번호를 변경하세요
- 실제 비밀번호는 `generate-sample-data.sh` 스크립트를 확인하거나 데이터베이스를 직접 조회하세요

**주의사항:**
- `.env` 파일이 설정되어 있어야 합니다
- 데이터베이스가 생성되어 있어야 합니다
- 기존 데이터와 충돌하지 않도록 `ON CONFLICT DO NOTHING` 사용

### 통합 Makefile

**Code를 만들어 낼 수 있는 방법 및 Script (Makefile 포함)**

프로젝트 루트에 통합 `Makefile`이 포함되어 있어 코드를 생성하고 빌드할 수 있습니다.

**위치:** `Record-BE/Makefile`

**Makefile이 포함된 이유:**
- 코드를 자동으로 생성하고 빌드할 수 있는 스크립트 제공
- 반복적인 빌드 작업을 간소화
- 프로젝트 재생성 시 일관된 빌드 프로세스 보장

**전체 Makefile 내용:**

```makefile
.PHONY: help be-build be-run be-test be-clean

# 기본 타겟
help:
	@echo "Record Project - Backend Makefile"
	@echo ""
	@echo "Backend 명령어:"
	@echo "  make be-build       - Backend 빌드"
	@echo "  make be-run         - Backend 실행"
	@echo "  make be-test        - Backend 테스트"
	@echo "  make be-clean       - Backend 정리"
	@echo ""
	@echo "  make help           - 이 도움말 표시"

# ============================================
# Backend 명령어
# ============================================

# Backend 의존성 다운로드
be-install:
	@echo "📦 Backend 의존성 다운로드 중..."
	@cd Record-BE/Record-BE && ./gradlew dependencies

# Backend 빌드
be-build:
	@echo "🔨 Backend 빌드 중..."
	@cd Record-BE/Record-BE && ./gradlew build

# Backend 실행
be-run:
	@echo "🚀 Backend 실행 중..."
	@cd Record-BE/Record-BE && ./gradlew bootRun

# Backend 테스트
be-test:
	@echo "🧪 Backend 테스트 실행 중..."
	@cd Record-BE/Record-BE && ./gradlew test

# Backend 정리
be-clean:
	@echo "🧹 Backend 빌드 산출물 정리 중..."
	@cd Record-BE/Record-BE && ./gradlew clean

# Backend JAR 생성
be-jar: be-build
	@echo "📦 Backend JAR 파일 생성 완료: Record-BE/Record-BE/build/libs/Record-BE-0.0.1-SNAPSHOT.jar"

# ============================================
# 전체 정리
# ============================================

# 전체 정리
clean: be-clean
	@echo "✅ Backend 정리 완료"
```

**사용 예시:**

```bash
# 프로젝트 루트에서

# 도움말 보기
make help

# Backend 빌드
make be-build

# Backend 실행
make be-run

# Backend 테스트
make be-test

# Backend 정리
make be-clean
```

### Backend 스크립트

`Record-BE/Record-BE/` 디렉토리에 포함된 스크립트:

#### 필수 스크립트

**1. `setup.sh` - Proto-system 자동 설치**
- 전체 시스템을 처음부터 설정하는 스크립트
- 사전 요구사항 확인, 환경 변수 설정, 데이터베이스 생성, 빌드까지 자동화
- **사용법:** `chmod +x setup.sh && ./setup.sh`

**2. `generate-sample-data.sh` - 샘플 데이터 생성**
- 개발 및 테스트를 위한 샘플 데이터 자동 생성
- 사용자, 티켓, 리뷰, 친구 관계 데이터 포함
- **사용법:** `chmod +x generate-sample-data.sh && ./generate-sample-data.sh`

**3. `build.gradle` - Backend 빌드 스크립트**
- Gradle 빌드 스크립트
- **사용법:** `cd Record-BE/Record-BE && ./gradlew build` 또는 프로젝트 루트에서 `make be-build`

#### 스크립트 실행 권한 부여

필수 스크립트를 실행하기 전에 실행 권한을 부여해야 합니다:

```bash
cd Record-BE/Record-BE
chmod +x setup.sh generate-sample-data.sh
```

## 추가 정보

### 프로젝트 구조

```
Record/
├── Record-BE/              # Backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/record/
│   │   │   │   ├── auth/          # 인증 관련
│   │   │   │   ├── user/          # 사용자 관리
│   │   │   │   ├── review/        # 리뷰 관리
│   │   │   │   ├── ocr/           # OCR 기능
│   │   │   │   ├── STTorText/     # STT 기능
│   │   │   │   ├── AWS/           # AWS S3 통합
│   │   │   │   └── ...
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/
│   ├── build.gradle
│   └── *.sh
```

## 라이선스

이 프로젝트의 라이선스는 프로젝트 소유자에게 문의하세요.
