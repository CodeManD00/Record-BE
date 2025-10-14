package com.example.record.auth;


import com.example.record.auth.dto.SignupRequest; // 추가
import com.example.record.auth.dto.LoginRequest;  // 추가
import com.example.record.user.User;
import com.example.record.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsById(request.getId())) {
            return ResponseEntity.badRequest().body("이미 사용 중인 아이디입니다.");
        }

        User user = User.builder()
                .id(request.getId()) // id를 id로 저장
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role("USER")
                .build();

        userRepository.save(user);

        return ResponseEntity.ok("회원가입이 완료되었습니다.");
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
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
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
