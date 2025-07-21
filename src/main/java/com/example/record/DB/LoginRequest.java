// LoginRequest: 로그인 시 클라이언트로부터 전달받는 이메일과 비밀번호 정보를 담는 DTO입니다.

package com.example.record.DB;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    private String email;     // 사용자 이메일
    private String password;  // 사용자 비밀번호
}
