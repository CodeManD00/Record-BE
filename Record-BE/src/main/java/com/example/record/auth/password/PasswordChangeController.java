package com.example.record.auth.password;

import com.example.record.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth/password")
@RequiredArgsConstructor
public class PasswordChangeController {

    private final PasswordChangeService passwordChangeService;

    /**
     * 비밀번호 변경
     * 
     * JWT 토큰 없이도 이전 비밀번호만으로 새로운 비밀번호로 변경 가능합니다.
     * 이전 비밀번호 확인으로 인증을 대체합니다.
     * 
     * @param req 비밀번호 변경 요청 (userId, oldPassword, newPassword 포함)
     * @return 비밀번호 변경 결과
     */
    @PostMapping("/change")
    public ResponseEntity<ApiResponse<?>> changePassword(
            @Valid @RequestBody PasswordChangeRequest req
    ) {
        log.info("비밀번호 변경 요청 받음: userId={}", req.getUserId());
        try {
            passwordChangeService.changePassword(req);
            log.info("비밀번호 변경 성공: userId={}", req.getUserId());
            return ResponseEntity.ok(
                    new ApiResponse<>(true, null, "비밀번호가 변경되었습니다.")
            );
        } catch (IllegalArgumentException e) {
            log.error("비밀번호 변경 실패: userId={}, error={}", req.getUserId(), e.getMessage());
            // 사용자를 찾을 수 없거나 현재 비밀번호가 틀린 경우
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, e.getMessage())
            );
        }
    }
}
