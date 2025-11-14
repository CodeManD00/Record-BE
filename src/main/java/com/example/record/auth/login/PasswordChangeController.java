package com.example.record.auth.login;

import com.example.record.auth.security.AuthUser;
import com.example.record.user.User;
import com.example.record.user.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class PasswordChangeController {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    /** 로그인한 사용자가 자신의 비밀번호 변경 */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody ChangePasswordRequest req
    ) {

        User me = authUser.getUser();

        User user = userRepository.findById(me.getId())
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        if (!encoder.matches(req.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.status(400).body("현재 비밀번호가 일치하지 않습니다.");
        }

        user.setPassword(encoder.encode(req.getNewPassword()));

        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    @Data
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
    }
}
