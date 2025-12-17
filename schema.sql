-- ============================================================================
-- Record - 데이터베이스 스키마 정의
-- ============================================================================
--
-- 이 SQL 파일은 프로젝트의 데이터베이스 스키마를 정의합니다.
-- - 사용자, 티켓, 리뷰, 친구 관계 등 모든 테이블 생성
-- - 프로젝트 재생성 시 데이터베이스 구조를 정의
--
-- 사용 방법:
--   psql -U recorduser -d recorddb -f "schema.sql"
--
-- ============================================================================

-- 1. users 테이블
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(15) PRIMARY KEY,
    email VARCHAR(30) UNIQUE NOT NULL,
    password VARCHAR(300) NOT NULL,
    nickname VARCHAR(30) NOT NULL,
    role VARCHAR(10) DEFAULT 'USER',
    favorite VARCHAR(255),
    is_account_private BOOLEAN DEFAULT FALSE,
    profile_image VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 2. friendships 테이블 (친구 관계)
CREATE TABLE IF NOT EXISTS friendships (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(15) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    friend_id VARCHAR(15) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, friend_id)
);

CREATE INDEX IF NOT EXISTS idx_friendships_user_id ON friendships(user_id);
CREATE INDEX IF NOT EXISTS idx_friendships_friend_id ON friendships(friend_id);

-- 3. tickets 테이블
CREATE TABLE IF NOT EXISTS tickets (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(15) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    performance_title VARCHAR(100) NOT NULL,
    venue VARCHAR(100),
    seat VARCHAR(50),
    artist VARCHAR(100),
    poster_url VARCHAR(400),
    genre VARCHAR(20),
    view_date DATE NOT NULL,
    image_url VARCHAR(400),
    image_prompt TEXT,
    review_text TEXT,
    is_public BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tickets_user_id ON tickets(user_id);

-- 4. reviews 테이블
CREATE TABLE IF NOT EXISTS reviews (
    id SERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    summary TEXT NOT NULL,
    keywords TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reviews_ticket_id ON reviews(ticket_id);

-- 5. questions_templates 테이블 (질문 템플릿)
CREATE TABLE IF NOT EXISTS questions_templates (
    id SERIAL PRIMARY KEY,
    template_text TEXT NOT NULL,
    category VARCHAR(50),
    genre VARCHAR(50)
);

-- 6. review_questions 테이블 (리뷰 질문)
CREATE TABLE IF NOT EXISTS review_questions (
    id SERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    template_id BIGINT NOT NULL REFERENCES questions_templates(id) ON DELETE CASCADE,
    display_order INTEGER NOT NULL,
    custom_text TEXT
);

-- 7. generated_image_url 테이블 (생성된 이미지)
CREATE TABLE IF NOT EXISTS generated_image_url (
    id SERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    style VARCHAR(30),
    is_selected BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_generated_images_review_id ON generated_image_url(review_id);

-- 8. ticket_likes 테이블 (티켓 좋아요)
CREATE TABLE IF NOT EXISTS ticket_likes (
    id SERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    user_id VARCHAR(15) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(ticket_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_ticket_likes_ticket_id ON ticket_likes(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_likes_user_id ON ticket_likes(user_id);

-- 9. user_custom_questions 테이블 (사용자 맞춤 질문)
CREATE TABLE IF NOT EXISTS user_custom_questions (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(15) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(50),
    genre VARCHAR(50),
    template_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_custom_questions_user_id ON user_custom_questions(user_id);

-- 10. email_verification 테이블 (이메일 인증)
CREATE TABLE IF NOT EXISTS email_verification (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 11. transcription 테이블 (STT 전사 결과)
CREATE TABLE IF NOT EXISTS transcription (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(15) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_name VARCHAR(255),
    result_text TEXT,
    summary TEXT,
    summary_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_transcription_user_id ON transcription(user_id);

-- 12. api_key 테이블 (사용자별 OpenAI API 키)
CREATE TABLE IF NOT EXISTS api_key (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255),
    api_key VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

-- 13. musical_db 테이블 (뮤지컬 정보)
CREATE TABLE IF NOT EXISTS musical_db (
    id SERIAL PRIMARY KEY,
    title VARCHAR(50) NOT NULL,
    summary TEXT,
    background VARCHAR(50),
    main_character_count INTEGER,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 14. musical_characters 테이블 (뮤지컬 캐릭터)
CREATE TABLE IF NOT EXISTS musical_characters (
    id SERIAL PRIMARY KEY,
    musical_id BIGINT REFERENCES musical_db(id) ON DELETE CASCADE,
    name VARCHAR(50),
    gender VARCHAR(10),
    age VARCHAR(255),
    occupation VARCHAR(50),
    description TEXT
);

-- 15. band_db 테이블 (밴드 정보)
CREATE TABLE IF NOT EXISTS band_db (
    id SERIAL PRIMARY KEY,
    title VARCHAR(50) NOT NULL,
    summary TEXT,
    background VARCHAR(50),
    main_member_count INTEGER,
    band_name VARCHAR(50) NOT NULL,
    band_name_meaning TEXT,
    band_symbol VARCHAR(255),
    poster_color VARCHAR(50),
    genre VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);

-- ============================================================================
-- 테이블 생성 완료
-- ============================================================================
