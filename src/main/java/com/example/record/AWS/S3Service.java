package com.example.record.AWS;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadFile(MultipartFile file) throws IOException {
        // S3 클라이언트 생성
        S3Client s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();

        // 파일 이름 생성
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        // 요청 객체 생성
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .acl("public-read") // 외부 접근 허용
                .contentType(file.getContentType())
                .build();

        // 업로드
        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

        // 파일 URL 반환
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + fileName;
    }
}
