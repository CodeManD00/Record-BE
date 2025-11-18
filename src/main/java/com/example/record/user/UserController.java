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

/**
 * 사용자 관련 컨트롤러
 *
 * 역할: 사용자 프로필 조회, 수정 등 사용자 관련 API 제공
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    /**
     * 현재 로그인한 사용자 프로필 조회 API
     *
     * @param authUser 현재 인증된 사용자 (SecurityContext에서 주입되는 AuthUser)
     * @return ApiResponse<UserProfileResponse> - 사용자 프로필 정보 (닉네임 포함)
     *
     * 응답 형식:
     * - 성공: { "success": true, "data": UserProfileResponse, "message": "프로필 조회 성공" }
     * - 실패: { "success": false, "data": null, "message": "에러 메시지" }
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> getMyProfile(
            @AuthenticationPrincipal AuthUser authUser) {
        try {
            if (authUser == null) {
                return ResponseEntity.badRequest().body(
                        new ApiResponse<>(false, null, "인증된 사용자 정보를 찾을 수 없습니다.")
                );
            }

            User user = authUser.getUser();   // ⭐ 실제 User 엔티티

            UserProfileResponse userProfile = new UserProfileResponse(
                    user.getId(),
                    user.getNickname(),              // name: 닉네임
                    "@" + user.getId(),             // username: @아이디
                    user.getEmail(),
                    user.getProfileImage(),         // 프로필 이미지 URL
                    null,                           // avatar (추후 확장용)
                    null,                           // bio (추후 확장용)
                    user.getCreatedAt() != null ? user.getCreatedAt().toString() : null,
                    user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null
            );

            return ResponseEntity.ok(
                    new ApiResponse<>(true, userProfile, "프로필 조회 성공")
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "프로필을 가져올 수 없습니다: " + e.getMessage())
            );
        }
    }

    /**
     * 프로필 이미지 업로드/변경 API
     *
     * 요청:
     * - Method: PUT
     * - URL: /users/me/profile-image
     * - Content-Type: multipart/form-data
     * - Body: file (이미지 파일)
     */
    @PutMapping(
            value = "/me/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<?>> uploadProfileImage(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestPart("file") MultipartFile file
    ) {
        try {
            if (authUser == null) {
                return ResponseEntity.badRequest().body(
                        new ApiResponse<>(false, null, "인증된 사용자 정보를 찾을 수 없습니다.")
                );
            }

            User user = authUser.getUser();   // ⭐

            User updated = userService.updateProfileImage(user, file);

            UserProfileResponse userProfile = new UserProfileResponse(
                    updated.getId(),
                    updated.getNickname(),
                    "@" + updated.getId(),
                    updated.getEmail(),
                    updated.getProfileImage(),   // 변경된 이미지 URL
                    null,
                    null,
                    updated.getCreatedAt() != null ? updated.getCreatedAt().toString() : null,
                    updated.getUpdatedAt() != null ? updated.getUpdatedAt().toString() : null
            );

            return ResponseEntity.ok(
                    new ApiResponse<>(true, userProfile, "프로필 이미지가 변경되었습니다.")
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "프로필 이미지를 변경할 수 없습니다: " + e.getMessage())
            );
        }
    }

    /**
     * 회원탈퇴 API
     *
     * @param authUser 현재 인증된 사용자 (SecurityContext에서 주입)
     * @param request 비밀번호 확인 요청
     * @return ApiResponse - 회원탈퇴 성공 메시지
     *
     * 응답 형식:
     * - 성공: { "success": true, "data": null, "message": "회원탈퇴가 완료되었습니다." }
     * - 실패: { "success": false, "data": null, "message": "에러 메시지" }
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<?>> deleteAccount(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody(required = false) DeleteAccountRequest request) {
        try {
            if (authUser == null) {
                return ResponseEntity.badRequest().body(
                        new ApiResponse<>(false, null, "인증된 사용자 정보를 찾을 수 없습니다.")
                );
            }

            User user = authUser.getUser();   // ⭐

            if (request != null && request.getPassword() != null && !request.getPassword().isBlank()) {
                if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                    return ResponseEntity.badRequest().body(
                            new ApiResponse<>(false, null, "비밀번호가 일치하지 않습니다.")
                    );
                }
            }

            userRepository.delete(user);

            return ResponseEntity.ok(
                    new ApiResponse<>(true, null, "회원탈퇴가 완료되었습니다.")
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "회원탈퇴 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }

    /**
     * 회원탈퇴 요청 DTO
     */
    @Getter
    @Setter
    public static class DeleteAccountRequest {
        @NotBlank(message = "비밀번호는 필수입니다.")
        private String password;
    }

    /**
     * 사용자 프로필 응답 DTO
     * 프론트엔드 UserProfile 인터페이스와 일치해야 함
     */
    public record UserProfileResponse(
            String id,
            String name,        // 닉네임 (nickname)
            String username,    // @아이디 형식
            String email,
            String profileImage,
            String avatar,
            String bio,
            String createdAt,
            String updatedAt
    ) {}
}
