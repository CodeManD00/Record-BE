#!/bin/bash

# DB URL 업데이트 스크립트
# 로컬 경로를 S3 URL로 변경합니다.

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 환경 변수 확인
if [ -z "$RecS3_BUCKET" ]; then
    echo -e "${RED}❌ RecS3_BUCKET 환경 변수가 설정되지 않았습니다.${NC}"
    exit 1
fi

# DB 연결 정보 확인
if [ -z "$DB_URL" ] || [ -z "$DB_USER" ] || [ -z "$DB_PASSWORD" ]; then
    echo -e "${YELLOW}⚠️  DB 연결 정보를 환경 변수에서 읽을 수 없습니다.${NC}"
    echo "다음 정보를 입력해주세요:"
    read -p "DB Host (예: record-db.czsw4i6quunb.ap-northeast-2.rds.amazonaws.com): " DB_HOST
    read -p "DB Port (기본값: 5432): " DB_PORT
    DB_PORT=${DB_PORT:-5432}
    read -p "DB Name (기본값: recorddb): " DB_NAME
    DB_NAME=${DB_NAME:-recorddb}
    read -p "DB User (기본값: recorduser): " DB_USER
    DB_USER=${DB_USER:-recorduser}
    read -sp "DB Password: " DB_PASSWORD
    echo ""
else
    # DB_URL에서 정보 추출 (jdbc:postgresql://host:port/dbname 형식)
    DB_HOST=$(echo $DB_URL | sed -n 's/.*:\/\/\([^:]*\):\([^\/]*\)\/\(.*\)/\1/p')
    DB_PORT=$(echo $DB_URL | sed -n 's/.*:\/\/\([^:]*\):\([^\/]*\)\/\(.*\)/\2/p')
    DB_NAME=$(echo $DB_URL | sed -n 's/.*:\/\/\([^:]*\):\([^\/]*\)\/\(.*\)/\3/p')
    
    # DB_URL 파싱이 실패한 경우 직접 추출
    if [ -z "$DB_HOST" ]; then
        DB_HOST=$(echo $DB_URL | sed 's/.*:\/\/\([^:]*\).*/\1/')
        DB_PORT=$(echo $DB_URL | sed 's/.*:\/\/[^:]*:\([^\/]*\).*/\1/')
        DB_NAME=$(echo $DB_URL | sed 's/.*:\/\/[^:]*:[^\/]*\/\(.*\)/\1/')
    fi
fi

# RecS3_BUCKET이 비어있으면 기본값 사용
if [ -z "$RecS3_BUCKET" ]; then
    RecS3_BUCKET="recording-buckett"
    echo -e "${YELLOW}⚠️  RecS3_BUCKET이 설정되지 않아 기본값을 사용합니다: $RecS3_BUCKET${NC}"
fi

BUCKET="$RecS3_BUCKET"
REGION="${AWS_REGION:-ap-northeast-2}"

echo -e "${GREEN}=== DB URL 업데이트 시작 ===${NC}"
echo "Bucket: $BUCKET"
echo "Region: $REGION"
echo "DB Host: $DB_HOST"
echo ""

# 임시 SQL 파일 생성
TEMP_SQL=$(mktemp)
cat > "$TEMP_SQL" <<EOF
-- users 테이블 업데이트
UPDATE users
SET profile_image = 
    CASE 
        WHEN profile_image LIKE '/uploads/profile-images/%' THEN
            'https://$BUCKET.s3.$REGION.amazonaws.com/profile-images/' || 
            SUBSTRING(profile_image FROM '/uploads/profile-images/(.*)')
        ELSE profile_image
    END
WHERE profile_image LIKE '/uploads/profile-images/%';

-- tickets 테이블 업데이트
UPDATE tickets
SET image_url = 
    CASE 
        WHEN image_url LIKE '/uploads/generated-images/%' THEN
            'https://$BUCKET.s3.$REGION.amazonaws.com/generated-images/' || 
            SUBSTRING(image_url FROM '/uploads/generated-images/(.*)')
        ELSE image_url
    END
WHERE image_url LIKE '/uploads/generated-images/%';

-- generated_image_url 테이블 업데이트
UPDATE generated_image_url
SET image_url = 
    CASE 
        WHEN image_url LIKE '/uploads/generated-images/%' THEN
            'https://$BUCKET.s3.$REGION.amazonaws.com/generated-images/' || 
            SUBSTRING(image_url FROM '/uploads/generated-images/(.*)')
        ELSE image_url
    END
WHERE image_url LIKE '/uploads/generated-images/%';

-- 결과 확인
SELECT 
    'users' as table_name,
    COUNT(*) as total_rows,
    COUNT(CASE WHEN profile_image LIKE 'https://%' THEN 1 END) as s3_urls,
    COUNT(CASE WHEN profile_image LIKE '/uploads/%' THEN 1 END) as local_urls
FROM users
UNION ALL
SELECT 
    'tickets' as table_name,
    COUNT(*) as total_rows,
    COUNT(CASE WHEN image_url LIKE 'https://%' THEN 1 END) as s3_urls,
    COUNT(CASE WHEN image_url LIKE '/uploads/%' THEN 1 END) as local_urls
FROM tickets
UNION ALL
SELECT 
    'generated_image_url' as table_name,
    COUNT(*) as total_rows,
    COUNT(CASE WHEN image_url LIKE 'https://%' THEN 1 END) as s3_urls,
    COUNT(CASE WHEN image_url LIKE '/uploads/%' THEN 1 END) as local_urls
FROM generated_image_url;
EOF

# PostgreSQL 연결 및 실행
export PGPASSWORD="$DB_PASSWORD"

echo -e "${YELLOW}DB 업데이트 실행 중...${NC}"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$TEMP_SQL"

# 임시 파일 삭제
rm "$TEMP_SQL"

echo ""
echo -e "${GREEN}=== DB URL 업데이트 완료 ===${NC}"

