package com.example.record.auth.password;

import com.example.record.auth.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/password")
@RequiredArgsConstructor
public class PasswordChangeController {

    private final PasswordChangeService passwordChangeService;

    @PostMapping("/change")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PasswordChangeRequest req
    ) {
        // ✅ 로그인 안 된 경우 방어
        if (authUser == null) {
            return ResponseEntity
                    .status(401)
                    .body("로그인이 필요합니다.");
        }

        passwordChangeService.changePassword(authUser.getUser(), req);
        return ResponseEntity.ok("비밀번호가 변경되었습니다.");
    }
}
