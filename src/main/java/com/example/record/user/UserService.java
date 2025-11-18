package com.example.record.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final LocalFileStorageService localFileStorageService;

    @Transactional
    public User updateProfileImage(User user, MultipartFile file) {
        validateImage(file);

        // 1) 로컬에 저장
        String imageUrl = localFileStorageService.saveProfileImage(user.getId(), file);

        // 2) User 엔티티에 URL 저장
        user.setProfileImage(imageUrl);

        // 3) DB 저장
        return userRepository.save(user);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어 있습니다.");
        }

        // 용량 제한 (예: 5MB)
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("이미지 용량은 5MB 이하여야 합니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }
    }
}
