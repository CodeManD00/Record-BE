package com.example.record.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    @NotBlank(message = "아이디는 필수입니다.")
    @Size(min = 3, max = 20, message = "아이디는 3~20자로 입력해주세요.")
    private String id;   // 아이디 (로그인용, 중복 불가)

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;      // 이메일 (중복 불가)

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "비밀번호는 8~64자로 입력해주세요.")
    private String password;   // 비밀번호 (BCrypt로 저장)

    // 닉네임은 중복 가능 + 필수
    @NotBlank(message = "닉네임은 필수입니다.")  // 추가!
    @Size(max = 30, message = "닉네임은 30자 이하로 입력해주세요.")
    private String nickname;   // 닉네임 (중복 허용, 필수 입력)
}
