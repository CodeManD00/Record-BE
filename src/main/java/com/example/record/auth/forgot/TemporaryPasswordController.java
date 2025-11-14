package com.example.record.auth.forgot;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/forgot")
@RequiredArgsConstructor
@Validated
public class TemporaryPasswordController {

    private final TemporaryPasswordService temporaryPasswordService;

    @PostMapping("/temporary-password")
    public ResponseEntity<?> sendTempPassword(@RequestBody TempPwRequest req) {
        temporaryPasswordService.issueAndSend(req.getEmail());
        return ResponseEntity.ok().build();
    }

    @Data
    public static class TempPwRequest {
        @NotBlank @Email
        private String email;
    }
}
