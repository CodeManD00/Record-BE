package com.example.record.DB;

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
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        // 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        // 사용자 존재 여부 및 비밀번호 확인
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).build();
        }

        // JWT 토큰 생성 및 반환
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        TokenResponse body = new TokenResponse(
                token,
                "Bearer",
                60L * 60 * 1000, // 현재 JwtUtil의 만료시간과 일치(1시간)
                user.getRole()
        );

        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + token)
                .body(body);
    }
}
