#!/bin/bash

# 직접 실행용 S3 업로드 스크립트
# 환경 변수를 직접 설정하거나 입력받아 실행합니다.

cd "$(dirname "$0")"

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== S3 이미지 업로드 ===${NC}"
echo ""

# 환경 변수 입력받기
read -p "AWS Access Key ID: " AWS_ACCESS_KEY
read -sp "AWS Secret Access Key: " AWS_SECRET_KEY
echo ""
read -p "S3 Bucket (기본값: recording-buckett): " BUCKET
BUCKET=${BUCKET:-recording-buckett}
read -p "AWS Region (기본값: ap-northeast-2): " REGION
REGION=${REGION:-ap-northeast-2}

export AWS_ACCESS_KEY_ID="$AWS_ACCESS_KEY"
export AWS_SECRET_ACCESS_KEY="$AWS_SECRET_KEY"
export AWS_DEFAULT_REGION="$REGION"

echo ""
echo -e "${YELLOW}[1/2] 프로필 이미지 업로드 중...${NC}"
aws s3 sync uploads/profile-images/ "s3://$BUCKET/profile-images/" \
    --exclude ".*" \
    --acl public-read \
    --cache-control "max-age=31536000"

PROFILE_COUNT=$(find uploads/profile-images -type f 2>/dev/null | wc -l | tr -d ' ')
echo -e "${GREEN}✅ 프로필 이미지 업로드 완료 ($PROFILE_COUNT개)${NC}"

echo ""
echo -e "${YELLOW}[2/2] 생성된 이미지 업로드 중...${NC}"
aws s3 sync uploads/generated-images/ "s3://$BUCKET/generated-images/" \
    --exclude ".*" \
    --acl public-read \
    --cache-control "max-age=31536000"

GENERATED_COUNT=$(find uploads/generated-images -type f 2>/dev/null | wc -l | tr -d ' ')
echo -e "${GREEN}✅ 생성된 이미지 업로드 완료 ($GENERATED_COUNT개)${NC}"

echo ""
echo -e "${GREEN}=== 업로드 완료 ===${NC}"

