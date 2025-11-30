-- S3 마이그레이션: DB URL 업데이트 스크립트
-- 로컬 경로(/uploads/...)를 S3 URL로 변경합니다.

-- 환경 변수에서 버킷 이름을 가져와야 합니다.
-- 사용 예: psql ... -v bucket_name=your-bucket-name -f update-db-urls.sql

-- 1. users 테이블의 profile_image 업데이트
-- 로컬 경로: /uploads/profile-images/filename.jpg
-- S3 URL: https://{bucket}.s3.ap-northeast-2.amazonaws.com/profile-images/filename.jpg
UPDATE users
SET profile_image = REPLACE(
    REPLACE(profile_image, '/uploads/profile-images/', ''),
    profile_image,
    CASE 
        WHEN profile_image LIKE '/uploads/profile-images/%' THEN
            'https://' || :'bucket_name' || '.s3.ap-northeast-2.amazonaws.com/profile-images/' || 
            SUBSTRING(profile_image FROM '/uploads/profile-images/(.*)')
        ELSE profile_image
    END
)
WHERE profile_image LIKE '/uploads/profile-images/%';

-- 2. tickets 테이블의 image_url 업데이트
UPDATE tickets
SET image_url = REPLACE(
    REPLACE(image_url, '/uploads/generated-images/', ''),
    image_url,
    CASE 
        WHEN image_url LIKE '/uploads/generated-images/%' THEN
            'https://' || :'bucket_name' || '.s3.ap-northeast-2.amazonaws.com/generated-images/' || 
            SUBSTRING(image_url FROM '/uploads/generated-images/(.*)')
        ELSE image_url
    END
)
WHERE image_url LIKE '/uploads/generated-images/%';

-- 3. generated_image_url 테이블의 image_url 업데이트
UPDATE generated_image_url
SET image_url = REPLACE(
    REPLACE(image_url, '/uploads/generated-images/', ''),
    image_url,
    CASE 
        WHEN image_url LIKE '/uploads/generated-images/%' THEN
            'https://' || :'bucket_name' || '.s3.ap-northeast-2.amazonaws.com/generated-images/' || 
            SUBSTRING(image_url FROM '/uploads/generated-images/(.*)')
        ELSE image_url
    END
)
WHERE image_url LIKE '/uploads/generated-images/%';

-- 업데이트 결과 확인
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

