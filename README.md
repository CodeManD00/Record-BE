# Record Project - Backend

공연 티켓 기록 및 리뷰 관리 시스템 - Spring Boot 기반 RESTful API 서버

## 빠른 시작

### 자동 설치
```bash
cd Record-BE
chmod +x setup.sh
./setup.sh
```

### 수동 설치
```bash
# 1. 환경 변수 설정
export DB_URL=jdbc:postgresql://localhost:5432/recorddb
export DB_USER=your_db_user
export DB_PASSWORD=your_secure_password
export JWT_SECRET=$(openssl rand -base64 32)

# 2. 데이터베이스 생성
psql -U postgres
CREATE DATABASE recorddb;
CREATE USER your_db_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE recorddb TO your_db_user;
\q

# 3. 실행
cd Record-BE/Record-BE
./gradlew bootRun
```

서버는 http://localhost:8080 에서 실행됩니다.

## 환경 변수 설정

### 필수 환경 변수

| 변수명 | 설명 | 예시 |
|--------|------|------|
| `DB_URL` | PostgreSQL 연결 URL | `jdbc:postgresql://localhost:5432/recorddb` |
| `DB_USER` | 데이터베이스 사용자명 | `your_db_user` |
| `DB_PASSWORD` | 데이터베이스 비밀번호 | `your_secure_password` |
| `JWT_SECRET` | JWT 시크릿 키 (최소 32바이트) | `openssl rand -base64 32`로 생성 |

### 선택적 환경 변수 (기능별)

| 기능 | 변수명 | 설명 |
|------|--------|------|
| **OCR** | `GOOGLE_APPLICATION_CREDENTIALS` | Google Cloud Vision API JSON 키 파일 경로 |
| **STT/이미지 생성** | `OPENAI_API_KEY` | OpenAI API 키 |
| **파일 저장** | `RecAWS_ACCESS_KEY_ID`<br>`RecAWS_SECRET_ACCESS_KEY`<br>`RecS3_BUCKET`<br>`AWS_REGION` | AWS S3 설정 |
| **이메일 인증** | `MAIL_USERNAME`<br>`MAIL_PASSWORD` | Gmail SMTP 설정 (앱 비밀번호 사용) |

**최소 설정**: 데이터베이스 + JWT 시크릿 키만으로도 기본 기능 사용 가능

## 프로젝트 구조

```
Record-BE/
├── src/main/java/com/example/record/
│   ├── auth/          # 인증 (JWT, 이메일, 소셜 로그인)
│   ├── user/          # 사용자 관리
│   ├── review/        # 리뷰 및 티켓 관리
│   ├── ocr/           # OCR 기능 (Google Cloud Vision)
│   ├── STTorText/     # STT 기능 (OpenAI Whisper)
│   ├── AWS/           # AWS S3 파일 저장
│   └── config/        # 설정 클래스
├── src/main/resources/
│   └── application.yml
└── build.gradle
```

## 기술 스택

- **Java 21**
- **Spring Boot 3.2.5**
- **PostgreSQL**
- **JWT (JJWT)**
- **AWS S3**
- **Google Cloud Vision API** (OCR)
- **OpenAI API** (Whisper, GPT, DALL-E)

## 주요 기능

- 사용자 인증 및 관리 (JWT, 이메일 인증)
- 티켓 CRUD 및 검색
- 리뷰 작성 및 관리
- OCR (티켓 이미지 텍스트 추출)
- STT (음성 텍스트 변환)
- AI 이미지 생성 (DALL-E)
- 친구 관리
- 파일 저장 (AWS S3)

## 빌드 및 실행

### 빌드
```bash
cd Record-BE/Record-BE
./gradlew build
```

### 실행
```bash
cd Record-BE/Record-BE
./gradlew bootRun
```

## API 문서

서버 실행 후 Swagger UI에서 API 문서 확인:
- http://localhost:8080/swagger-ui.html

## 샘플 데이터 생성

개발 및 테스트를 위한 샘플 데이터 생성:
```bash
cd Record-BE
chmod +x generate-sample-data.sh
./generate-sample-data.sh
```

생성되는 데이터:
- 사용자 3명 (관리자 1명, 일반 사용자 2명)
- 뮤지컬 정보 3개
- 티켓 데이터 3개
- 리뷰 데이터 2개
- 친구 관계 1개

## 데이터베이스

- **PostgreSQL 12 이상** (권장: PostgreSQL 14+)
- 기본 포트: `5432`
- 기본 데이터베이스명: `recorddb`

### 주요 테이블
- `users` - 사용자 정보
- `tickets` - 티켓 정보
- `reviews` - 리뷰 정보
- `friendships` - 친구 관계
- `email_verifications` - 이메일 인증
- `ticket_likes` - 티켓 좋아요

## 오픈소스 라이선스

이 프로젝트는 다음 오픈소스를 사용합니다:

- **Spring Boot** (3.2.5) - Apache License 2.0
- **PostgreSQL JDBC Driver** (42.6.0) - BSD License
- **SpringDoc OpenAPI** (2.5.0) - Apache License 2.0
- **JJWT** (0.11.5) - Apache License 2.0
- **AWS SDK for Java v2** (2.25.11) - Apache License 2.0
- **Google Cloud Vision** (3.36.0) - Apache License 2.0
- **Lombok** - MIT License
- **JUnit 5** - Eclipse Public License 2.0

모든 오픈소스는 상업적 사용이 가능하며, 각 라이선스를 준수합니다.

### 상용 API 서비스 

- **OpenAI API** (Whisper, GPT, DALL-E) 
  - 사용을 위해 API 키와 결제 계정이 필요합니다
  - 오픈소스 라이브러리가 아니라 REST API를 통해 호출하는 서비스입니다

- **Google Cloud Vision API** 
  - 사용을 위해 Google Cloud 계정과 결제 설정이 필요합니다
  - SDK는 오픈소스이지만, API 자체는 상용 서비스입니다

## 보안 주의사항

- **절대** 기본 비밀번호(`recordpass` 등)를 사용하지 마세요
- 강력한 비밀번호 사용 (최소 12자 이상)
- `.env` 파일은 Git에 커밋하지 마세요
- JWT 시크릿 키는 `openssl rand -base64 32`로 생성하세요
- 프로덕션 환경에서는 환경 변수 관리 도구 사용 권장

## 라이선스
코드만두에 문의하세요.
