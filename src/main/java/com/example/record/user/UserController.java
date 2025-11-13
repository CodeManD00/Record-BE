package com.example.record.user;

import com.example.record.common.ApiResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;

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

    /**
     * 현재 로그인한 사용자 프로필 조회 API
     * 
     * @param user 현재 인증된 사용자 (SecurityContext에서 주입)
     * @return ApiResponse<UserProfileResponse> - 사용자 프로필 정보 (닉네임 포함)
     * 
     * 응답 형식:
     * - 성공: { "success": true, "data": UserProfileResponse, "message": "프로필 조회 성공" }
     * - 실패: { "success": false, "data": null, "message": "에러 메시지" }
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> getMyProfile(
            @AuthenticationPrincipal User user) {
        try {
            // SecurityContext에서 주입된 User 객체 사용
            if (user == null) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "인증된 사용자 정보를 찾을 수 없습니다.")
                );
            }

            // UserProfileResponse 생성 (프론트엔드 UserProfile 형식에 맞춤)
            UserProfileResponse userProfile = new UserProfileResponse(
                    user.getId(),
                    user.getNickname(),  // name 필드에 nickname 사용
                    "@" + user.getId(), // username은 @아이디 형식
                    user.getEmail(),
                    null,  // profileImage (추후 추가 가능)
                    null,  // avatar (추후 추가 가능)
                    null,  // bio (추후 추가 가능)
                    user.getCreatedAt() != null ? user.getCreatedAt().toString() : null,
                    user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null
            );

            // ApiResponse로 감싸서 반환
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
     * 회원탈퇴 API
     * 
     * @param user 현재 인증된 사용자 (SecurityContext에서 주입)
     * @param request 비밀번호 확인 요청
     * @return ApiResponse - 회원탈퇴 성공 메시지
     * 
     * 응답 형식:
     * - 성공: { "success": true, "data": null, "message": "회원탈퇴가 완료되었습니다." }
     * - 실패: { "success": false, "data": null, "message": "에러 메시지" }
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<?>> deleteAccount(
            @AuthenticationPrincipal User user,
            @RequestBody(required = false) DeleteAccountRequest request) {
        try {
            // SecurityContext에서 주입된 User 객체 사용
            if (user == null) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "인증된 사용자 정보를 찾을 수 없습니다.")
                );
            }

            // 비밀번호 확인 (보안을 위해)
            if (request != null && request.getPassword() != null && !request.getPassword().isBlank()) {
                if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                    return ResponseEntity.badRequest().body(
                        new ApiResponse<>(false, null, "비밀번호가 일치하지 않습니다.")
                    );
                }
            }

            // 사용자 삭제 (FK 제약조건에 의해 관련 데이터도 함께 삭제될 수 있음)
            userRepository.delete(user);

            // ApiResponse로 감싸서 반환
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

