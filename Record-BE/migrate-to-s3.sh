#!/bin/bash

# S3 마이그레이션 스크립트
# 로컬 /uploads 폴더의 이미지를 S3로 업로드하고 DB URL을 업데이트합니다.

set -e  # 에러 발생 시 스크립트 중단

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 스크립트가 있는 디렉토리로 이동
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# .env 파일이 있으면 로드
if [ -f .env ]; then
    echo -e "${YELLOW}📄 .env 파일에서 환경 변수 로드 중...${NC}"
    export $(grep -v '^#' .env | xargs)
fi

# 환경 변수 확인 및 설정
if [ -z "$RecAWS_ACCESS_KEY_ID" ]; then
    echo -e "${YELLOW}⚠️  RecAWS_ACCESS_KEY_ID가 설정되지 않았습니다.${NC}"
    read -sp "RecAWS_ACCESS_KEY_ID를 입력하세요: " RecAWS_ACCESS_KEY_ID
    echo ""
    export RecAWS_ACCESS_KEY_ID
fi

if [ -z "$RecAWS_SECRET_ACCESS_KEY" ]; then
    echo -e "${YELLOW}⚠️  RecAWS_SECRET_ACCESS_KEY가 설정되지 않았습니다.${NC}"
    read -sp "RecAWS_SECRET_ACCESS_KEY를 입력하세요: " RecAWS_SECRET_ACCESS_KEY
    echo ""
    export RecAWS_SECRET_ACCESS_KEY
fi

if [ -z "$RecS3_BUCKET" ]; then
    echo -e "${YELLOW}⚠️  RecS3_BUCKET이 설정되지 않았습니다.${NC}"
    read -p "RecS3_BUCKET을 입력하세요 (기본값: recording-buckett): " RecS3_BUCKET
    RecS3_BUCKET=${RecS3_BUCKET:-recording-buckett}
    export RecS3_BUCKET
fi

# AWS CLI 설치 확인
if ! command -v aws &> /dev/null; then
    echo -e "${RED}❌ AWS CLI가 설치되지 않았습니다.${NC}"
    echo "설치 방법: https://aws.amazon.com/cli/"
    exit 1
fi

# AWS 자격 증명 설정
export AWS_ACCESS_KEY_ID="$RecAWS_ACCESS_KEY_ID"
export AWS_SECRET_ACCESS_KEY="$RecAWS_SECRET_ACCESS_KEY"
export AWS_DEFAULT_REGION="${AWS_REGION:-ap-northeast-2}"

BUCKET="$RecS3_BUCKET"
UPLOADS_DIR="uploads"

echo -e "${GREEN}=== S3 마이그레이션 시작 ===${NC}"
echo "Bucket: $BUCKET"
echo "Region: $AWS_DEFAULT_REGION"
echo ""

# 1. 프로필 이미지 업로드
echo -e "${YELLOW}[1/2] 프로필 이미지 업로드 중...${NC}"
if [ -d "$UPLOADS_DIR/profile-images" ]; then
    aws s3 sync "$UPLOADS_DIR/profile-images" "s3://$BUCKET/profile-images/" \
        --exclude ".*" \
        --acl public-read \
        --metadata-directive REPLACE \
        --cache-control "max-age=31536000"
    
    PROFILE_COUNT=$(find "$UPLOADS_DIR/profile-images" -type f | wc -l | tr -d ' ')
    echo -e "${GREEN}✅ 프로필 이미지 $PROFILE_COUNT개 업로드 완료${NC}"
else
    echo -e "${YELLOW}⚠️  profile-images 폴더가 없습니다.${NC}"
fi

# 2. 생성된 이미지 업로드
echo -e "${YELLOW}[2/2] 생성된 이미지 업로드 중...${NC}"
if [ -d "$UPLOADS_DIR/generated-images" ]; then
    aws s3 sync "$UPLOADS_DIR/generated-images" "s3://$BUCKET/generated-images/" \
        --exclude ".*" \
        --acl public-read \
        --metadata-directive REPLACE \
        --cache-control "max-age=31536000"
    
    GENERATED_COUNT=$(find "$UPLOADS_DIR/generated-images" -type f | wc -l | tr -d ' ')
    echo -e "${GREEN}✅ 생성된 이미지 $GENERATED_COUNT개 업로드 완료${NC}"
else
    echo -e "${YELLOW}⚠️  generated-images 폴더가 없습니다.${NC}"
fi

echo ""
echo -e "${GREEN}=== S3 업로드 완료 ===${NC}"
echo ""
echo -e "${YELLOW}다음 단계: DB URL 업데이트${NC}"
echo "다음 명령어를 실행하여 DB URL을 업데이트하세요:"
echo "  psql -h <DB_HOST> -U <DB_USER> -d <DB_NAME> -f update-db-urls.sql"
echo ""
echo "또는 다음 스크립트를 실행하세요:"
echo "  ./update-db-urls.sh"

