#!/bin/bash

# AWS CLI 설정 파일 생성 스크립트

mkdir -p ~/.aws

cat > ~/.aws/credentials <<EOF
[default]
aws_access_key_id = ${RecAWS_ACCESS_KEY_ID}
aws_secret_access_key = ${RecAWS_SECRET_ACCESS_KEY}
EOF

cat > ~/.aws/config <<EOF
[default]
region = ap-northeast-2
output = json
EOF

echo "✅ AWS CLI 설정 완료"
echo "이제 다음 명령어로 업로드할 수 있습니다:"
echo "  cd Record-BE"
echo "  ./quick-upload.sh"

