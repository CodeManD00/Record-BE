package com.example.record.auth.forgot;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AccountRecoveryController {

    private final AccountRecoveryService service;

    /** 1) 아이디 찾기: 이메일로 아이디 전송 */
    @PostMapping("/forgot-id")
    public ResponseEntity<?> forgotId(@RequestBody @Valid ForgotIdRequest req) {
        service.sendLoginIdByEmail(req);
        // 존재 유무를 노출하지 않기 위해 항상 동일 메시지 반환
        return ResponseEntity.ok().body("해당 이메일로 안내 메일을 확인해 주세요.");
    }

    /** 2) 비밀번호 재설정 링크 발송 */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody @Valid ForgotPasswordRequest req) {
        service.sendResetPasswordLink(req);
        return ResponseEntity.ok().body("비밀번호 재설정 링크를 이메일로 보냈습니다.");
    }

    /** 3) 새 비밀번호로 업데이트 */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordRequest req) {
        service.resetPassword(req);
        return ResponseEntity.ok().body("비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.");
    }
}
