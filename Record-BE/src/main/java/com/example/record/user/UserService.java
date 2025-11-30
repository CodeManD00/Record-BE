package com.example.record.user;

import com.example.record.AWS.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final LocalFileStorageService localFileStorageService;
    private final S3Service s3Service;

    @Transactional
    public User updateProfile(User user, UserController.UpdateProfileRequest req) {

        if (req.getNickname() != null) {
            user.setNickname(req.getNickname());
        }
        if (req.getEmail() != null) {
            user.setEmail(req.getEmail());
        }
        if (req.getIsAccountPrivate() != null) {
            user.setIsAccountPrivate(req.getIsAccountPrivate());
        }

        return userRepository.save(user);
    }

    /**
     * 닉네임만 변경하는 메서드
     * 
     * @param user 변경할 사용자
     * @param nickname 새로운 닉네임
     * @return 업데이트된 사용자 정보
     */
    @Transactional
    public User updateNickname(User user, String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        }

        if (nickname.length() > 30) {
            throw new IllegalArgumentException("닉네임은 30자 이하여야 합니다.");
        }

        user.setNickname(nickname.trim());
        return userRepository.save(user);
    }

    // ────────────────────────────────────────────
    //  프로필 이미지 변경 (기존 이미지 삭제 + 새 이미지 저장)
    // ────────────────────────────────────────────
    @Transactional
    public User updateProfileImage(User user, MultipartFile file) {
        validateImage(file);

        // 1) 기존 이미지 삭제
        deleteOldImage(user.getProfileImage());

        // 2) 새 이미지 저장
        String imageUrl = localFileStorageService.saveProfileImage(user.getId(), file);

        // 3) User 엔티티에 새로운 URL 반영
        user.setProfileImage(imageUrl);

        // 4) DB 저장
        return userRepository.save(user);
    }

    // ────────────────────────────────────────────
    //  기존 프로필 이미지 파일 삭제
    // ────────────────────────────────────────────
    private void deleteOldImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;

        // S3 URL인 경우 S3에서 삭제, 로컬 경로인 경우 무시 (마이그레이션 중일 수 있음)
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            // S3 URL인 경우
            s3Service.deleteFile(imageUrl);
        } else {
            // 로컬 경로인 경우 (기존 데이터 마이그레이션 중일 수 있음)
            // S3로 완전히 전환되면 이 부분은 제거 가능
            System.out.println("⚠ 로컬 경로 이미지는 삭제하지 않음 (S3로 마이그레이션 필요): " + imageUrl);
        }
    }

    // ────────────────────────────────────────────
    //  이미지 유효성 검사
    // ────────────────────────────────────────────
    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어 있습니다.");
        }

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
