package com.example.record.auth.forgot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ResetPasswordRequest {
    @NotBlank
    private String token;     // 이메일로 간 링크의 토큰
    @NotBlank @Size(min = 8, max = 64)
    private String newPassword;
}