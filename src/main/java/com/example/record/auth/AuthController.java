package com.example.record.auth;

import com.example.record.auth.dto.SignupRequest;
import com.example.record.auth.dto.LoginRequest;
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

    private static final long ACCESS_TOKEN_EXPIRES_MS = 1000L * 60 * 60;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsById(request.getId())) {
            return ResponseEntity.badRequest().body("이미 사용 중인 아이디입니다.");
        }

        User user = User.builder()
                .id(request.getId())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role("USER")
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        return ResponseEntity.ok(new TokenResponse(
                token,
                "Bearer",
                ACCESS_TOKEN_EXPIRES_MS,
                user.getRole()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("아이디를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("비밀번호가 일치하지 않습니다.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        return ResponseEntity.ok(new TokenResponse(
                token,
                "Bearer",
                ACCESS_TOKEN_EXPIRES_MS,
                user.getRole()
        ));
    }
}
