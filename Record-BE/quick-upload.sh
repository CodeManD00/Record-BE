#!/bin/bash

# 빠른 S3 업로드 스크립트
# 환경 변수를 매핑하여 한 번에 실행

cd "$(dirname "$0")"

# 환경 변수 매핑
export AWS_ACCESS_KEY_ID="$RecAWS_ACCESS_KEY_ID"
export AWS_SECRET_ACCESS_KEY="$RecAWS_SECRET_ACCESS_KEY"
export AWS_DEFAULT_REGION="${AWS_REGION:-ap-northeast-2}"

BUCKET="${RecS3_BUCKET:-recording-buckett}"

echo "=== S3 업로드 시작 ==="
echo "Bucket: $BUCKET"
echo "Region: $AWS_DEFAULT_REGION"
echo ""

# 프로필 이미지 업로드
echo "[1/2] 프로필 이미지 업로드 중..."
aws s3 sync uploads/profile-images/ "s3://$BUCKET/profile-images/" \
    --acl public-read \
    --cache-control "max-age=31536000" \
    --exclude ".*" \
    --region "$AWS_DEFAULT_REGION"

PROFILE_COUNT=$(find uploads/profile-images -type f 2>/dev/null | wc -l | tr -d ' ')
echo "✅ 프로필 이미지 업로드 완료 ($PROFILE_COUNT개)"
echo ""

# 생성된 이미지 업로드
echo "[2/2] 생성된 이미지 업로드 중..."
aws s3 sync uploads/generated-images/ "s3://$BUCKET/generated-images/" \
    --acl public-read \
    --cache-control "max-age=31536000" \
    --exclude ".*" \
    --region "$AWS_DEFAULT_REGION"

GENERATED_COUNT=$(find uploads/generated-images -type f 2>/dev/null | wc -l | tr -d ' ')
echo "✅ 생성된 이미지 업로드 완료 ($GENERATED_COUNT개)"
echo ""

echo "=== 업로드 완료 ==="

