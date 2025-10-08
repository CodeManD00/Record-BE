package com.example.record.auth;

import com.example.record.auth.dto.LoginRequest;
import com.example.record.auth.dto.SignupRequest;
import com.example.record.user.User;
import com.example.record.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final long ACCESS_TOKEN_EXPIRES_MS = 1000L * 60 * 60; // JwtUtil과 동일 시간(1시간)

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body("이미 사용 중인 아이디입니다.");
        }

        // User 엔티티에 role 필드가 없으므로 빌더에서 role 사용 X
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        userRepository.save(user);

        // 기본 권한은 USER로 발급
        String role = "USER";
        String token = jwtUtil.generateToken(user.getUsername(), role);

        return ResponseEntity.ok(new TokenResponse(
                token,
                "Bearer",
                ACCESS_TOKEN_EXPIRES_MS,
                role
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("아이디를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("비밀번호가 일치하지 않습니다.");
        }

        String role = "USER"; // User 엔티티에 role 없으므로 기본값 사용
        String token = jwtUtil.generateToken(user.getUsername(), role);

        return ResponseEntity.ok(new TokenResponse(
                token,
                "Bearer",
                ACCESS_TOKEN_EXPIRES_MS,
                role
        ));
    }
}
