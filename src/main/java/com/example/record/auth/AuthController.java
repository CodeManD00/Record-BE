package com.example.record.auth;


import com.example.record.auth.dto.SignupRequest; // 추가
import com.example.record.auth.dto.LoginRequest;  // 추가
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
        if (userRepository.existsById(request.getId())) {
            return ResponseEntity.badRequest().body("이미 사용 중인 아이디입니다.");
        }

        // User 엔티티에 role 필드가 없으므로 빌더에서 role 사용 X
        User user = User.builder()
                .id(request.getId()) // id를 id로 저장
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role("USER")
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
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        /**
         * 로그인 시 사용자 조회
         * 
         * 변경 사항:
         * - findByUsername → findById로 변경
         * - 이유: username 용어를 id로 통일하기 때문
         * 
         * 이제 사용자 ID로 로그인하게 됩니다.
         */
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("아이디를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("비밀번호가 일치하지 않습니다.");
        }

        /**
         * JWT 토큰 생성
         * 
         * 변경 사항:
         * - user.getUsername() → user.getId()로 변경
         * - 이유: username 용어를 id로 통일하기 때문
         */
        String token = jwtUtil.generateToken(user.getId());

        return ResponseEntity.ok(new TokenResponse(
                token,
                "Bearer",
                3600000L, // 시간 (밀리초)
                user.getRole() // TokenResponse에서는 4개의 필드 받는데 AuthController에서는 1개만 보내고 있길래 수정했음
        ));
    }
}
