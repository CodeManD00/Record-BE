# Record by CODEMAND00

공연 티켓 기록 및 리뷰 관리 시스템 - Full Stack 프로젝트

## 프로젝트 개요

- **Backend**: Spring Boot 3.2.5 (Java 21), PostgreSQL, JWT 인증
- **Frontend**: React Native 0.81.0 (TypeScript)
- **주요 기능**: 티켓 관리, OCR, STT, AI 이미지 생성, 리뷰 작성

## 목차

1. [설치하기](#설치하기)
2. [빌드하기](#빌드하기)
3. [실행하기](#실행하기)
4. [테스트하기](#테스트하기)
5. [데이터](#데이터)
6. [의존성](#의존성)
7. [오픈소스](#오픈소스)
8. [프로젝트 구조](#프로젝트-구조)

## 설치하기

### 사용자 입장 (빠른 시작)

자동 설치 스크립트를 사용하여 환경 설정, Backend 의존성 설치 및 빌드까지 완료합니다.  
Frontend는 개발 환경에서 별도 빌드 단계가 없으며, 실행 시 자동으로 빌드됩니다.

```bash
cd Record-BE/Record-BE
chmod +x setup.sh
./setup.sh
```

설치 완료 후 다음 단계를 진행하세요:

**1. Frontend 의존성 설치 (Frontend 사용 시)**
```bash
cd Record-FE && npm install
cd Record-FE/ios && bundle exec pod install  # iOS만 (macOS)
```

**2. 샘플 데이터 생성 (선택)**
```bash
chmod +x generate-sample-data.sh
./generate-sample-data.sh
```

**생성되는 샘플 데이터:**
- 사용자 3명 (admin@example.com, user1@example.com, user2@example.com)
- 비밀번호: `password123` (모든 사용자 동일)
- 뮤지컬 정보 3개 (레미제라블, 시카고, 위키드)
- 티켓 데이터 3개
- 리뷰 데이터 2개
- 친구 관계 1개

---

### 개발자 입장 (수동 설치)

#### 1. 환경 변수 설정
`.env` 파일 생성:
```bash
DB_URL=jdbc:postgresql://localhost:5432/recorddb
DB_USER=recorduser
DB_PASSWORD=your_password
JWT_SECRET=$(openssl rand -base64 32)
```

**기능을 위한 환경 변수 (선택):**
- `OPENAI_API_KEY`: STT, DALLE 이미지 생성 기능
- `RecAWS_ACCESS_KEY_ID`, `RecAWS_SECRET_ACCESS_KEY`: S3 파일 저장
- `GOOGLE_APPLICATION_CREDENTIALS`: OCR 기능
- `MAIL_USERNAME`, `MAIL_PASSWORD`: 이메일 인증

#### 2. 데이터베이스 설정
```bash
psql -U postgres
CREATE DATABASE recorddb;
CREATE USER recorduser WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE recorddb TO recorduser;
```

#### 3. 의존성 설치 및 빌드 준비
```bash
# Backend
make be-install

# Frontend
cd Record-FE && npm install
cd Record-FE/ios && bundle exec pod install  # iOS만 (macOS)
```

## 빌드하기

### Backend
```bash
make be-build
```

Frontend는 개발 환경 기준으로 `npm run ios` 실행 시  
필요한 빌드 과정이 자동으로 수행되므로, 별도의 빌드 명령을 사용하지 않습니다.

## 실행하기

```bash
# Backend
make be-run

# Frontend
# 1. 개발 서버 실행 (Metro)
cd Record-FE && npm start

# 2. 앱 실행 (빌드 포함)
cd Record-FE && npm run ios 
```

## 테스트하기

```bash
# Backend
make be-test

# Frontend
make fe-test
```


## 데이터

- **데이터베이스**: PostgreSQL 12+
- **주요 테이블**: users, tickets, reviews, friendships, musical_db
- **외부 서비스**: AWS S3, Google Cloud Vision API, OpenAI API

## 의존성

자세한 의존성 목록은 다음 파일을 확인하세요:
- Backend: `Record-BE/Record-BE/build.gradle`
- Frontend: `Record-FE/package.json`

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
