#!/bin/bash

# 샘플 데이터 생성 스크립트

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# 환경 변수 로드
if [ -f .env ]; then
    set -a
    source .env
    set +a
fi

# 데이터베이스 정보 확인
DB_HOST=$(echo $DB_URL | sed -n 's/.*:\/\/\([^:]*\):\([^\/]*\)\/\(.*\)/\1/p' || echo "localhost")
DB_PORT=$(echo $DB_URL | sed -n 's/.*:\/\/\([^:]*\):\([^\/]*\)\/\(.*\)/\2/p' || echo "5432")
DB_NAME=$(echo $DB_URL | sed -n 's/.*:\/\/\([^:]*\):\([^\/]*\)\/\(.*\)/\3/p' || echo "recorddb")

if [ -z "$DB_USER" ] || [ -z "$DB_PASSWORD" ]; then
    echo -e "${RED}❌ DB_USER 또는 DB_PASSWORD가 설정되지 않았습니다.${NC}"
    echo ".env 파일을 확인하세요."
    exit 1
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}샘플 데이터 생성${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

TEMP_SQL=$(mktemp)

cat > "$TEMP_SQL" << 'EOF'
-- 샘플 데이터 생성 스크립트
-- 비밀번호: password123 (BCrypt 해시)

INSERT INTO users (id, email, password, nickname, role, favorite, is_account_private, created_at) VALUES
('admin001', 'admin@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '관리자', 'ADMIN', '뮤지컬', false, NOW()),
('user001', 'user1@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '사용자1', 'USER', '뮤지컬', false, NOW()),
('user002', 'user2@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '사용자2', 'USER', '밴드', false, NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO musical_db (title, summary, background, main_character_count, created_at) VALUES
('레미제라블', '빅토르 위고의 소설을 원작으로 한 뮤지컬', '19세기 프랑스', 10, NOW()),
('시카고', '1920년대 시카고를 배경으로 한 범죄 뮤지컬', '1920년대 미국', 8, NOW()),
('위키드', '오즈의 마법사를 다른 관점에서 본 뮤지컬', '오즈의 나라', 6, NOW())
ON CONFLICT DO NOTHING;

INSERT INTO tickets (user_id, performance_title, venue, seat, artist, genre, view_date, image_url, is_public, created_at) VALUES
('user001', '레미제라블', '샤롯데씨어터', '1층 A열 10번', '주연배우', 'MUSICAL', '2024-01-15', 'https://example.com/ticket1.jpg', true, NOW()),
('user001', '시카고', '블루스퀘어', '2층 B열 5번', '주연배우', 'MUSICAL', '2024-02-20', 'https://example.com/ticket2.jpg', true, NOW()),
('user002', '레미제라블', '샤롯데씨어터', '1층 C열 15번', '주연배우', 'MUSICAL', '2024-01-20', 'https://example.com/ticket3.jpg', false, NOW())
ON CONFLICT DO NOTHING;

INSERT INTO reviews (ticket_id, summary, keywords, created_at) VALUES
(1, '훌륭한 공연이었습니다! 음악과 연기가 모두 뛰어났어요.', '감동,음악,연기', NOW()),
(2, '시카고의 분위기가 정말 좋았습니다. 재미있게 봤어요.', '재미,분위기,춤', NOW())
ON CONFLICT DO NOTHING;

INSERT INTO friendships (user_id, friend_id, status, created_at) VALUES
('user001', 'user002', 'ACCEPTED', NOW())
ON CONFLICT (user_id, friend_id) DO NOTHING;

INSERT INTO ticket_likes (ticket_id, user_id, created_at) VALUES
(1, 'user002', NOW()),
(2, 'user002', NOW())
ON CONFLICT (ticket_id, user_id) DO NOTHING;

SELECT 'users' as table_name, COUNT(*) as count FROM users
UNION ALL
SELECT 'musical_db', COUNT(*) FROM musical_db
UNION ALL
SELECT 'tickets', COUNT(*) FROM tickets
UNION ALL
SELECT 'reviews', COUNT(*) FROM reviews
UNION ALL
SELECT 'friendships', COUNT(*) FROM friendships
UNION ALL
SELECT 'ticket_likes', COUNT(*) FROM ticket_likes;
EOF

echo -e "${YELLOW}샘플 데이터를 생성합니다...${NC}"

export PGPASSWORD="$DB_PASSWORD"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$TEMP_SQL"

rm "$TEMP_SQL"

echo ""
echo -e "${GREEN}✅ 샘플 데이터 생성 완료!${NC}"
echo ""
echo -e "${BLUE}생성된 데이터:${NC}"
echo "- 사용자: 3명 (admin@example.com, user1@example.com, user2@example.com)"
echo "- 비밀번호: password123 (모든 사용자 동일)"
echo "- 뮤지컬: 3개 (레미제라블, 시카고, 위키드)"
echo "- 티켓: 3개"
echo "- 리뷰: 2개"
echo "- 친구 관계: 1개"
