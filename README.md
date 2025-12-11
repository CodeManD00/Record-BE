# Record by CODEMANDU

공연 티켓 기록 및 리뷰 관리 시스템 - Full Stack 프로젝트

## 빠른 시작

```bash
cd Record-BE/Record-BE
chmod +x setup.sh
./setup.sh
```

## 프로젝트 개요

- **Backend**: Spring Boot 3.2.5 (Java 21), PostgreSQL, JWT 인증
- **Frontend**: React Native 0.81.0 (TypeScript)
- **주요 기능**: 티켓 관리, OCR, STT, AI 이미지 생성, 리뷰 작성

## 목차

1. [빌드하기](#빌드하기)
2. [설치하기](#설치하기)
3. [테스트하기](#테스트하기)
4. [샘플 데이터](#샘플-데이터)
5. [데이터](#데이터)
6. [오픈소스](#오픈소스)

## 빌드하기

### 전체 프로젝트 빌드
```bash
make build-all
```

### Backend 빌드
```bash
make be-build
# 또는
cd Record-BE/Record-BE && ./gradlew build
```

### Frontend 빌드
```bash
make fe-install
# 또는
cd Record-FE && npm install
```

## 설치하기

### 자동 설치
```bash
cd Record-BE/Record-BE
chmod +x setup.sh
./setup.sh
```

### 수동 설치

#### 1. 환경 변수 설정
`.env` 파일 생성:
```bash
DB_URL=jdbc:postgresql://localhost:5432/recorddb
DB_USER=recorduser
DB_PASSWORD=your_password
JWT_SECRET=$(openssl rand -base64 32)
```

#### 2. 데이터베이스 설정
```bash
psql -U postgres
CREATE DATABASE recorddb;
CREATE USER recorduser WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE recorddb TO recorduser;
```

#### 3. Backend 실행
```bash
cd Record-BE/Record-BE
make be-run
# 또는
./gradlew bootRun
```

#### 4. Frontend 실행
```bash
cd Record-FE
npm install
npm run ios
```

### 기능을 위한 환경 변수
- `OPENAI_API_KEY`: STT, DALLE 이미지 생성 기능
- `RecAWS_ACCESS_KEY_ID`, `RecAWS_SECRET_ACCESS_KEY`: S3 파일 저장
- `GOOGLE_APPLICATION_CREDENTIALS`: OCR 기능
- `MAIL_USERNAME`, `MAIL_PASSWORD`: 이메일 인증

## 테스트하기

```bash
# 전체 테스트
make test-all

# Backend 테스트
make be-test

# Frontend 테스트
make fe-test
```

## 샘플 데이터

### sh 파일로 샘플 데이터 생성 
```bash
cd Record-BE/Record-BE
chmod +x generate-sample-data.sh
./generate-sample-data.sh
```

### 생성되는 데이터
- 사용자 3명 (admin@example.com, user1@example.com, user2@example.com)
- 비밀번호: `password123` (모든 사용자 동일)
- 뮤지컬 정보 3개 (레미제라블, 시카고, 위키드)
- 티켓 데이터 3개
- 리뷰 데이터 2개
- 친구 관계 1개

## 데이터

- **데이터베이스**: PostgreSQL 12+
- **주요 테이블**: users, tickets, reviews, friendships, musical_db
- **외부 서비스**: AWS S3, Google Cloud Vision API, OpenAI API

## 오픈소스

### Backend
- **Spring Boot** 3.2.5 - Apache License 2.0
- **PostgreSQL JDBC Driver** 42.6.0 - BSD License
- **JJWT** 0.11.5 - Apache License 2.0
- **AWS SDK** 2.25.11 - Apache License 2.0
- **Google Cloud Vision** 3.36.0 - Apache License 2.0

### Frontend
- **React Native** 0.81.0 - MIT License
- **React** 19.1.0 - MIT License
- **React Navigation** 7.x - MIT License
- **Jotai** 2.15.0 - MIT License
- **Axios** 1.13.2 - MIT License
- **TypeScript** 5.8.3 - Apache License 2.0

## Scripts (sh 파일)

### setup.sh
전체 시스템 자동 설치 스크립트
```bash
cd Record-BE/Record-BE
./setup.sh
```

### generate-sample-data.sh
샘플 데이터 생성 스크립트
```bash
cd Record-BE/Record-BE
./generate-sample-data.sh
```

### Makefile
통합 빌드 스크립트
```bash
make help  # 사용 가능한 명령어 확인
```

## 프로젝트 구조

```
Record-BE/
├── src/
│   ├── main/java/com/example/record/
│   │   ├── auth/          # 인증
│   │   ├── user/           # 사용자 관리
│   │   ├── review/         # 리뷰 관리
│   │   ├── ocr/            # OCR 기능
│   │   ├── STTorText/      # STT 기능
│   │   └── AWS/            # S3 통합
│   └── resources/
│       └── application.yml
├── setup.sh
├── generate-sample-data.sh
└── Makefile
```
