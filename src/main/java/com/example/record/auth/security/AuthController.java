package com.example.record.auth.security;

import com.example.record.auth.jwt.JwtUtil;
import com.example.record.auth.jwt.TokenResponse;
import com.example.record.auth.login.SignupRequest;
import com.example.record.auth.login.LoginRequest;
import com.example.record.common.ApiResponse;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /** 회원가입 */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<?>> signup(@Valid @RequestBody SignupRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "이미 사용 중인 이메일입니다.")
            );
        }

        if (userRepository.existsById(request.getId())) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "이미 사용 중인 아이디입니다.")
            );
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

        TokenResponse tokenResponse = new TokenResponse(
                token, "Bearer", jwtUtil.getExpirationMs(), user.getRole()
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, tokenResponse, "회원가입 성공")
        );
    }

    /** 로그인 */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@Valid @RequestBody LoginRequest request) {

        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("아이디를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "비밀번호가 일치하지 않습니다.")
            );
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        TokenResponse tokenResponse = new TokenResponse(
                token, "Bearer", jwtUtil.getExpirationMs(), user.getRole()
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, tokenResponse, "로그인 성공")
        );
    }

    /** 로그인한 사용자 정보 조회 */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> getCurrentUser(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser authUser) {

        if (authUser == null) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "인증된 사용자 정보를 찾을 수 없습니다.")
            );
        }

        User user = authUser.getUser();

        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole()
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, userResponse, "사용자 정보 조회 성공")
        );
    }

    /** 로그아웃 */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, null, "로그아웃 성공")
        );
    }

    public record UserResponse(
            String id,
            String email,
            String nickname,
            String role
    ) {}
}
