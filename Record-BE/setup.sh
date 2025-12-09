#!/bin/bash

# ============================================================================
# Record Project - Proto-system 설치 스크립트
# ============================================================================
#
# 주요 기능:
# 1. 사전 요구사항 확인
#    - Java 21 이상 설치 확인
#    - PostgreSQL 설치 확인
#
# 2. 환경 변수 자동 설정
#    - .env 파일 자동 생성
#    - 데이터베이스 연결 정보 입력 받기
#    - JWT 시크릿 키 자동 생성
#
# 3. 데이터베이스 자동 설정
#    - PostgreSQL 데이터베이스 생성
#    - 데이터베이스 사용자 생성 및 권한 부여
#
# 4. Backend 자동 설치 및 빌드
#    - Gradle 의존성 자동 다운로드
#    - Backend 프로젝트 자동 빌드
#
# 5. 데이터베이스 스키마 초기화
#    - SQL 스크립트 실행 또는 JPA 자동 생성
#
# 사용 방법:
#   cd Record-BE/Record-BE
#   chmod +x setup.sh
#   ./setup.sh
#
# Proto-system 설치 방법 및 Script 포함
#   - Git clone 후 README를 보고 바로 재생성 가능하도록 자동화


set -e  # 에러 발생 시 스크립트 중단

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 스크립트가 있는 디렉토리로 이동
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Record Project - Proto-system 설치${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 1. 사전 요구사항 확인
echo -e "${YELLOW}[1/7] 사전 요구사항 확인 중...${NC}"

# Java 확인
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java가 설치되지 않았습니다.${NC}"
    echo "Java 21 이상이 필요합니다."
    echo "설치 방법:"
    echo "  macOS: brew install openjdk@21"
    echo "  Linux: sudo apt install openjdk-21-jdk"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo -e "${RED}❌ Java 21 이상이 필요합니다. 현재 버전: $JAVA_VERSION${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Java 버전 확인: $(java -version 2>&1 | head -n 1)${NC}"

# PostgreSQL 확인
if ! command -v psql &> /dev/null; then
    echo -e "${YELLOW}⚠️  PostgreSQL이 설치되지 않았습니다.${NC}"
    echo "설치 방법:"
    echo "  macOS: brew install postgresql@14"
    echo "  Linux: sudo apt install postgresql postgresql-contrib"
    echo ""
    read -p "계속하시겠습니까? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo -e "${GREEN}✅ PostgreSQL 확인 완료${NC}"
fi

echo ""

# 2. 환경 변수 설정
echo -e "${YELLOW}[2/7] 환경 변수 설정 중...${NC}"

# .env 파일이 없으면 생성
if [ ! -f .env ]; then
    echo -e "${BLUE}.env 파일이 없습니다. 생성합니다...${NC}"
    
    # 데이터베이스 정보 입력
    read -p "PostgreSQL Host (기본값: localhost): " DB_HOST
    DB_HOST=${DB_HOST:-localhost}
    
    read -p "PostgreSQL Port (기본값: 5432): " DB_PORT
    DB_PORT=${DB_PORT:-5432}
    
    read -p "Database Name (기본값: recorddb): " DB_NAME
    DB_NAME=${DB_NAME:-recorddb}
    
    read -p "Database User (기본값: recorduser): " DB_USER
    DB_USER=${DB_USER:-recorduser}
    
    read -sp "Database Password: " DB_PASSWORD
    echo ""
    
    # JWT 시크릿 키 생성
    JWT_SECRET=$(openssl rand -base64 32)
    
    # .env 파일 생성
    cat > .env << EOF
# 필수 설정
DB_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
DB_USER=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}
JWT_SECRET=${JWT_SECRET}

# 선택적 설정 (기능 사용 시 설정)
# OPENAI_API_KEY=sk-...
# RecAWS_ACCESS_KEY_ID=AKIA...
# RecAWS_SECRET_ACCESS_KEY=...
# RecS3_BUCKET=your-bucket-name
# AWS_REGION=ap-northeast-2
# GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
# MAIL_USERNAME=your-email@gmail.com
# MAIL_PASSWORD=your-app-password
EOF
    
    echo -e "${GREEN}✅ .env 파일 생성 완료${NC}"
    echo -e "${YELLOW}⚠️  선택적 환경 변수는 .env 파일을 직접 수정하여 추가하세요.${NC}"
else
    echo -e "${GREEN}✅ .env 파일이 이미 존재합니다.${NC}"
fi

# 환경 변수 로드
set -a
source .env
set +a

echo ""

# 3. 데이터베이스 설정
echo -e "${YELLOW}[3/7] 데이터베이스 설정 중...${NC}"

# PostgreSQL이 실행 중인지 확인
if command -v psql &> /dev/null; then
    if psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -lqt 2>/dev/null | cut -d \| -f 1 | grep -qw "$DB_NAME"; then
        echo -e "${GREEN}✅ 데이터베이스 '$DB_NAME'가 이미 존재합니다.${NC}"
    else
        echo -e "${BLUE}데이터베이스 생성 중...${NC}"
        
        # 데이터베이스 생성 SQL
        PGPASSWORD=postgres psql -h "$DB_HOST" -p "$DB_PORT" -U postgres << EOF || true
CREATE DATABASE ${DB_NAME};
CREATE USER ${DB_USER} WITH PASSWORD '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON DATABASE ${DB_NAME} TO ${DB_USER};
\c ${DB_NAME}
GRANT ALL ON SCHEMA public TO ${DB_USER};
EOF
        
        echo -e "${GREEN}✅ 데이터베이스 생성 완료${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  PostgreSQL이 설치되지 않아 데이터베이스 설정을 건너뜁니다.${NC}"
    echo "수동으로 데이터베이스를 생성하세요:"
    echo "  CREATE DATABASE ${DB_NAME};"
    echo "  CREATE USER ${DB_USER} WITH PASSWORD '${DB_PASSWORD}';"
    echo "  GRANT ALL PRIVILEGES ON DATABASE ${DB_NAME} TO ${DB_USER};"
fi

echo ""

# 4. Backend 의존성 설치
echo -e "${YELLOW}[4/7] Backend 의존성 설치 중...${NC}"
./gradlew dependencies --no-daemon
echo -e "${GREEN}✅ Backend 의존성 설치 완료${NC}"
echo ""

# 5. Backend 빌드
echo -e "${YELLOW}[5/7] Backend 빌드 중...${NC}"
./gradlew build -x test --no-daemon
echo -e "${GREEN}✅ Backend 빌드 완료${NC}"
echo ""

# 6. 데이터베이스 스키마 초기화
echo -e "${YELLOW}[6/7] 데이터베이스 스키마 초기화 중...${NC}"

if [ -f "SQL for local DB" ]; then
    echo -e "${BLUE}SQL 스크립트를 실행합니다...${NC}"
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "SQL for local DB" || {
        echo -e "${YELLOW}⚠️  SQL 스크립트 실행 중 오류가 발생했습니다.${NC}"
        echo "JPA의 ddl-auto: update 설정으로 자동 생성됩니다."
    }
else
    echo -e "${YELLOW}⚠️  SQL 스크립트 파일을 찾을 수 없습니다.${NC}"
    echo "JPA의 ddl-auto: update 설정으로 자동 생성됩니다."
fi

echo -e "${GREEN}✅ 데이터베이스 스키마 초기화 완료${NC}"
echo ""

# 7. 설치 완료
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✅ Proto-system 설치 완료!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}다음 단계:${NC}"
echo ""
echo "1. 환경 변수 확인:"
echo "   cat .env"
echo ""
echo "2. Backend 실행:"
echo "   make run"
echo "   또는"
echo "   ./gradlew bootRun"
echo ""
echo "3. Swagger UI 접속:"
echo "   http://localhost:8080/swagger-ui.html"
echo ""
echo "4. 샘플 데이터 생성 (선택사항):"
echo "   ./generate-sample-data.sh"
echo ""

