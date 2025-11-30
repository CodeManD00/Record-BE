// S3Service: AWS S3에 파일을 업로드하고, 업로드된 파일의 공개 URL을 반환하는 서비스 클래스입니다.


package com.example.record.AWS;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class S3Service {

    // application.yml에서 S3 관련 설정 주입
    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private S3Client s3Client;

    private S3Client getS3Client() {
        if (s3Client == null) {
            s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
        }
        return s3Client;
    }

    /**
     * MultipartFile을 받아 AWS S3에 업로드하고 업로드된 파일의 URL을 반환
     * @param file 업로드할 파일
     * @return 업로드된 파일의 공개 URL
     */
    public String uploadFile(MultipartFile file) throws IOException {
        return uploadFile(file, null);
    }

    /**
     * MultipartFile을 받아 AWS S3에 업로드하고 업로드된 파일의 URL을 반환
     * @param file 업로드할 파일
     * @param folderPath S3 내 폴더 경로 (예: "profile-images", "generated-images")
     * @return 업로드된 파일의 공개 URL
     */
    public String uploadFile(MultipartFile file, String folderPath) throws IOException {
        // 고유한 파일 이름 생성 (UUID + 원래 파일명)
        String fileName = UUID.randomUUID() + "_" + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        return uploadFile(file, folderPath, fileName);
    }

    /**
     * MultipartFile을 받아 AWS S3에 업로드하고 업로드된 파일의 URL을 반환 (파일명 지정)
     * @param file 업로드할 파일
     * @param folderPath S3 내 폴더 경로 (예: "profile-images", "generated-images")
     * @param fileName 지정할 파일명
     * @return 업로드된 파일의 공개 URL
     */
    public String uploadFile(MultipartFile file, String folderPath, String fileName) throws IOException {
        // 폴더 경로가 있으면 추가
        String key = folderPath != null && !folderPath.isEmpty() 
                ? folderPath + "/" + fileName 
                : fileName;

        // S3에 업로드할 요청 객체 생성
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .acl("public-read") // 업로드된 파일을 외부에서 접근 가능하도록 설정
                .contentType(file.getContentType())
                .build();

        // 실제 파일 업로드 수행
        getS3Client().putObject(request, RequestBody.fromBytes(file.getBytes()));

        // 업로드된 파일의 공개 URL 반환
        String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        log.info("Uploaded file to S3: {}", url);
        return url;
    }

    /**
     * 바이트 배열을 S3에 업로드
     * @param imageBytes 업로드할 이미지 바이트 배열
     * @param fileName 파일명
     * @param folderPath S3 내 폴더 경로
     * @param contentType Content-Type (예: "image/png")
     * @return 업로드된 파일의 공개 URL
     */
    public String uploadBytes(byte[] imageBytes, String fileName, String folderPath, String contentType) throws IOException {
        // 폴더 경로가 있으면 추가
        String key = folderPath != null && !folderPath.isEmpty() 
                ? folderPath + "/" + fileName 
                : fileName;

        // S3에 업로드할 요청 객체 생성
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .acl("public-read")
                .contentType(contentType)
                .build();

        // 실제 파일 업로드 수행
        getS3Client().putObject(request, RequestBody.fromBytes(imageBytes));

        // 업로드된 파일의 공개 URL 반환
        String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        log.info("Uploaded bytes to S3: {}", url);
        return url;
    }

    /**
     * S3에서 파일 삭제
     * @param url 삭제할 파일의 S3 URL
     */
    public void deleteFile(String url) {
        if (url == null || url.isBlank()) {
            return;
        }

        try {
            // URL에서 key 추출
            // 예: https://bucket.s3.region.amazonaws.com/folder/file.jpg
            String key = url.replace("https://" + bucket + ".s3." + region + ".amazonaws.com/", "");
            
            if (key.isEmpty()) {
                log.warn("Could not extract key from URL: {}", url);
                return;
            }

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            getS3Client().deleteObject(deleteRequest);
            log.info("Deleted file from S3: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", url, e);
            // 삭제 실패해도 기능 자체는 계속 진행
        }
    }

    /**
     * 프로필 이미지 저장 (userId 기반 파일명 생성)
     * @param userId 사용자 ID
     * @param file 업로드할 파일
     * @return 업로드된 파일의 공개 URL
     */
    public String saveProfileImage(String userId, MultipartFile file) throws IOException {
        // 파일 확장자 추출
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 파일명: userId_타임스탬프_UUID.ext
        String filename = userId + "_" + Instant.now().toEpochMilli() + "_" + UUID.randomUUID() + ext;

        return uploadFile(file, "profile-images", filename);
    }

    /**
     * 생성된 이미지 저장 (바이트 배열)
     * @param imageBytes 이미지 바이트 배열
     * @return 업로드된 파일의 공개 URL
     */
    public String saveGeneratedImage(byte[] imageBytes) throws IOException {
        String filename = "cropped_" + Instant.now().toEpochMilli() + "_" + UUID.randomUUID() + ".png";
        return uploadBytes(imageBytes, filename, "generated-images", "image/png");
    }

    /**
     * S3 연결 및 버킷 접근 권한 확인
     * @return 연결 성공 여부
     */
    public boolean testConnection() {
        try {
            // 설정 정보 확인
            if (accessKey == null || accessKey.isBlank()) {
                log.error("❌ S3 Access Key가 설정되지 않았습니다.");
                return false;
            }
            if (secretKey == null || secretKey.isBlank()) {
                log.error("❌ S3 Secret Key가 설정되지 않았습니다.");
                return false;
            }
            if (bucket == null || bucket.isBlank()) {
                log.error("❌ S3 Bucket이 설정되지 않았습니다.");
                return false;
            }
            if (region == null || region.isBlank()) {
                log.error("❌ S3 Region이 설정되지 않았습니다.");
                return false;
            }

            // 버킷 접근 권한 확인 (HeadBucket 요청)
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucket)
                    .build();

            getS3Client().headBucket(headBucketRequest);
            
            log.info("✅ S3 연결 성공 - Bucket: {}, Region: {}", bucket, region);
            return true;
        } catch (NoSuchBucketException e) {
            log.error("❌ S3 버킷을 찾을 수 없습니다: {}", bucket);
            return false;
        } catch (S3Exception e) {
            log.error("❌ S3 연결 실패: {} (Status: {})", e.getMessage(), e.statusCode());
            return false;
        } catch (Exception e) {
            log.error("❌ S3 연결 확인 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }

    /**
     * S3 설정 정보 반환 (마스킹된 형태)
     * @return 설정 정보 문자열
     */
    public String getConfigInfo() {
        String maskedAccessKey = (accessKey != null && accessKey.length() > 8)
                ? accessKey.substring(0, 4) + "..." + accessKey.substring(accessKey.length() - 4)
                : "NOT_SET";
        String maskedSecretKey = (secretKey != null && secretKey.length() > 8)
                ? secretKey.substring(0, 4) + "..." + secretKey.substring(secretKey.length() - 4)
                : "NOT_SET";
        
        return String.format("Bucket: %s, Region: %s, AccessKey: %s, SecretKey: %s",
                bucket != null ? bucket : "NOT_SET",
                region != null ? region : "NOT_SET",
                maskedAccessKey,
                maskedSecretKey);
    }
}

