// AuthController: 사용자 회원가입과 로그인 요청을 처리하고 JWT 토큰을 발급하는 인증 컨트롤러입니다.

package com.example.record.DB;

import com.example.record.DB.User;
import com.example.record.DB.UserRepository;
import com.example.record.DB.SignupRequest;
import com.example.record.DB.LoginRequest;
import com.example.record.DB.JwtUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 회원가입 요청 처리
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("이미 사용 중인 이메일입니다.");
        }

        // 사용자 정보 생성 및 저장 (비밀번호는 암호화하여 저장)
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role("USER") // 기본 역할은 USER
                .build();

        userRepository.save(user);
        return ResponseEntity.ok("회원가입 성공");
    }

    // 로그인 요청 처리 및 JWT 토큰 발급
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        // 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // 사용자 존재 여부 및 비밀번호 확인
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // JWT 토큰 생성 및 반환
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return ResponseEntity.ok(token);
    }
}
