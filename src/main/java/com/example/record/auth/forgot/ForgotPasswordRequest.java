package com.example.record.auth.forgot;
// 지금은 안씀 미래를 위해
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter @Setter
public class ForgotPasswordRequest {
    @NotBlank @Size(min = 3, max = 15)
    private String id;        // 가입 아이디
    @NotBlank @Email
    private String email;     // 가입 이메일
}