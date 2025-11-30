package com.example.record.user;

import com.example.record.AWS.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocalFileStorageService {

    private final S3Service s3Service;

    public String saveProfileImage(String userId, MultipartFile file) {
        try {
            String url = s3Service.saveProfileImage(userId, file);
            log.info("Saved profile image for user {} to S3: {}", userId, url);
            return url;
        } catch (IOException e) {
            log.error("Failed to save profile image for user {} to S3", userId, e);
            throw new RuntimeException("프로필 이미지를 저장할 수 없습니다.", e);
        }
    }
}
