package com.example.record.user;

import com.example.record.auth.security.AuthUser;
import com.example.record.common.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final FriendshipService friendshipService;

    // ────────────────────────────────
    // Request DTO
    // ────────────────────────────────
    @Getter
    @Setter
    public static class UpdateProfileRequest {
        private String nickname;
        private String email;
        private Boolean isAccountPrivate;
    }

    @Getter
    @Setter
    public static class UpdateNicknameRequest {
        @NotBlank
        private String userId;
        @NotBlank
        private String nickname;
    }

    @Getter
    @Setter
    public static class DeleteAccountRequest {
        @NotBlank
        private String password;
    }

    // ────────────────────────────────
    // 1. 내 프로필 조회
    // ────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> getMyProfile(
            @AuthenticationPrincipal AuthUser authUser) {

        if (authUser == null) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "인증된 사용자 정보를 찾을 수 없습니다.")
            );
        }

        User user = authUser.getUser();
        return ResponseEntity.ok(
                new ApiResponse<>(true, new UserProfileResponse(user), "프로필 조회 성공")
        );
    }

    // ────────────────────────────────
    // 2. 프로필 정보 변경 (닉네임/이메일/비공개 여부)
    // ────────────────────────────────
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<?>> updateProfile(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody UpdateProfileRequest req
    ) {

        if (authUser == null) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "인증된 사용자 정보를 찾을 수 없습니다.")
            );
        }

        User updated = userService.updateProfile(authUser.getUser(), req);

        return ResponseEntity.ok(
                new ApiResponse<>(true, new UserProfileResponse(updated), "프로필이 수정되었습니다.")
        );
    }

    // ────────────────────────────────
    // 3. 닉네임 변경 (JWT 토큰 불필요)
    // ────────────────────────────────
    @PatchMapping("/nickname")
    public ResponseEntity<ApiResponse<?>> updateNickname(
            @RequestBody UpdateNicknameRequest req
    ) {
        // 사용자 ID 유효성 검사
        if (req.getUserId() == null || req.getUserId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "사용자 ID를 입력해주세요.")
            );
        }

        // 사용자 존재 여부 확인
        User user = userRepository.findById(req.getUserId().trim())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 닉네임 유효성 검사
        if (req.getNickname() == null || req.getNickname().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "닉네임을 입력해주세요.")
            );
        }

        String trimmedNickname = req.getNickname().trim();
        if (trimmedNickname.length() > 30) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "닉네임은 30자 이하여야 합니다.")
            );
        }

        User updated = userService.updateNickname(user, trimmedNickname);

        return ResponseEntity.ok(
                new ApiResponse<>(true, new UserProfileResponse(updated), "닉네임이 변경되었습니다.")
        );
    }

    // ────────────────────────────────
    // 4. 프로필 이미지 업로드 (JWT 토큰 불필요, 기존 이미지 덮어씌우기)
    // ────────────────────────────────
    @PutMapping(
            value = "/me/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<?>> uploadProfileImage(
            @RequestPart("userId") String userId,
            @RequestPart("file") MultipartFile file
    ) {
        // 사용자 ID 유효성 검사
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "사용자 ID를 입력해주세요.")
            );
        }

        // 사용자 존재 여부 확인
        User user = userRepository.findById(userId.trim())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 프로필 이미지 업데이트 (기존 이미지 덮어씌우기)
        // - 기존 이미지가 있으면 자동으로 삭제됨 (UserService.updateProfileImage 내부 처리)
        // - 새 이미지 저장 및 DB 업데이트
        User updated = userService.updateProfileImage(user, file);
        
        return ResponseEntity.ok(
                new ApiResponse<>(true, new UserProfileResponse(updated), "프로필 이미지가 변경되었습니다.")
        );
    }

    // ────────────────────────────────
    // 5. 사용자 ID로 검색 (친구 검색용)
    // ────────────────────────────────
    @GetMapping("/search/{userId}")
    public ResponseEntity<ApiResponse<?>> searchUser(
            @PathVariable("userId") String userId,
            @RequestHeader(value = "X-User-Id", required = false) String searcherId) {
        // 검색 대상 사용자 조회
        User user = userRepository.findById(userId.trim())
                .orElse(null);

        // 사용자를 찾을 수 없는 경우 404 반환
        if (user == null) {
            return ResponseEntity.status(404).body(
                    new ApiResponse<>(false, null, "사용자를 찾을 수 없습니다.")
            );
        }

        // 비공개 계정이고 본인이 아니면 검색 불가 (403 Forbidden)
        if (user.getIsAccountPrivate() != null && user.getIsAccountPrivate()) {
            if (searcherId == null || !searcherId.equals(userId)) {
                return ResponseEntity.status(403).body(
                        new ApiResponse<>(false, null, "비공개 계정입니다.")
                );
            }
        }

        // 친구 관계 상태 확인 (검색자가 있는 경우)
        String friendshipStatus = null;
        if (searcherId != null && !searcherId.equals(userId)) {
            var friendship = friendshipService.getFriendship(searcherId, userId);
            if (friendship.isPresent()) {
                friendshipStatus = friendship.get().getStatus();
            }
        }

        // 응답 생성
        UserSearchResponse response = new UserSearchResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImage(),
                user.getIsAccountPrivate(),
                friendshipStatus
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, response, "사용자 검색 성공")
        );
    }

    // ────────────────────────────────
    // 6. 회원탈퇴
    // ────────────────────────────────
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<?>> deleteAccount(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody(required = false) DeleteAccountRequest req
    ) {

        if (authUser == null) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "인증된 사용자 정보를 찾을 수 없습니다.")
            );
        }

        User user = authUser.getUser();

        if (req != null && req.getPassword() != null) {
            if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body(
                        new ApiResponse<>(false, null, "비밀번호가 일치하지 않습니다.")
                );
            }
        }

        userRepository.delete(user);
        return ResponseEntity.ok(
                new ApiResponse<>(true, null, "회원탈퇴가 완료되었습니다.")
        );
    }

    // ────────────────────────────────
    // Response DTO
    // ────────────────────────────────
    public record UserProfileResponse(
            String id,
            String nickname,
            String email,
            String profileImage,
            Boolean isAccountPrivate,
            String createdAt,
            String updatedAt
    ) {
        public UserProfileResponse(User user) {
            this(
                    user.getId(),
                    user.getNickname(),
                    user.getEmail(),
                    user.getProfileImage(),
                    user.getIsAccountPrivate(),

                    user.getCreatedAt() != null ? user.getCreatedAt().toString() : null,
                    user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null
            );
        }
    }

    // ────────────────────────────────
    // Response DTO - 사용자 검색
    // ────────────────────────────────
    public record UserSearchResponse(
            String id,
            String nickname,
            String profileImage,
            Boolean isAccountPrivate,
            String friendshipStatus  // PENDING, ACCEPTED, REJECTED, null (친구 관계 없음)
    ) {}
}
