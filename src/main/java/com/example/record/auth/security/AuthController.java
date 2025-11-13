
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

/**
 * 인증 관련 컨트롤러
 * 
 * 역할: 회원가입, 로그인 등 인증 관련 API 제공
 * 
 * 응답 형식:
 * - 모든 응답을 ApiResponse로 감싸서 반환
 * - 프론트엔드의 apiClient.ts가 기대하는 success, data, message 구조 제공
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 회원가입 API
     * 
     * @param request 회원가입 요청 정보 (아이디, 이메일, 비밀번호, 닉네임)
     * @return ApiResponse<TokenResponse> - 성공 시 토큰 정보 포함, 실패 시 에러 메시지
     * 
     * 응답 형식:
     * - 성공: { "success": true, "data": TokenResponse, "message": "회원가입 성공" }
     * - 실패: { "success": false, "data": null, "message": "에러 메시지" }
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<?>> signup(@Valid @RequestBody SignupRequest request) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, null, "이미 사용 중인 이메일입니다.")
            );
        }
        
        // 아이디 중복 확인
        if (userRepository.existsById(request.getId())) {
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, null, "이미 사용 중인 아이디입니다.")
            );
        }

        // 사용자 생성 및 저장
        User user = User.builder()
                .id(request.getId())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role("USER")
                .build();

        userRepository.save(user);

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        // TokenResponse 생성
        TokenResponse tokenResponse = new TokenResponse(
                token,
                "Bearer",
                jwtUtil.getExpirationMs(),
                user.getRole()
        );

        // ApiResponse로 감싸서 반환
        return ResponseEntity.ok(
            new ApiResponse<>(true, tokenResponse, "회원가입 성공")
        );
    }

    /**
     * 로그인 API
     * 
     * @param request 로그인 요청 정보 (아이디, 비밀번호)
     * @return ApiResponse<TokenResponse> - 성공 시 토큰 정보 포함, 실패 시 에러 메시지
     * 
     * 응답 형식:
     * - 성공: { "success": true, "data": TokenResponse, "message": "로그인 성공" }
     * - 실패: { "success": false, "data": null, "message": "에러 메시지" }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@Valid @RequestBody LoginRequest request) {
        // 사용자 조회
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("아이디를 찾을 수 없습니다."));

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, null, "비밀번호가 일치하지 않습니다.")
            );
        }

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        // TokenResponse 생성
        TokenResponse tokenResponse = new TokenResponse(
                token,
                "Bearer",
                jwtUtil.getExpirationMs(),
                user.getRole()
        );

        // ApiResponse로 감싸서 반환
        return ResponseEntity.ok(
            new ApiResponse<>(true, tokenResponse, "로그인 성공")
        );
    }

    /**
     * 현재 로그인한 사용자 정보 조회 API
     * 
     * @param user 현재 인증된 사용자 (SecurityContext에서 주입)
     * @return ApiResponse<UserResponse> - 사용자 정보 (닉네임 포함)
     * 
     * 응답 형식:
     * - 성공: { "success": true, "data": UserResponse, "message": "사용자 정보 조회 성공" }
     * - 실패: { "success": false, "data": null, "message": "에러 메시지" }
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> getCurrentUser(
            @org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        try {
            // SecurityContext에서 주입된 User 객체 사용
            if (user == null) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, null, "인증된 사용자 정보를 찾을 수 없습니다.")
                );
            }

            // UserResponse 생성 (닉네임 포함)
            UserResponse userResponse = new UserResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getNickname(),
                    user.getRole()
            );

            // ApiResponse로 감싸서 반환
            return ResponseEntity.ok(
                new ApiResponse<>(true, userResponse, "사용자 정보 조회 성공")
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, null, "사용자 정보를 가져올 수 없습니다: " + e.getMessage())
            );
        }
    }

    /**
     * 사용자 정보 응답 DTO
     */
    public record UserResponse(
            String id,
            String email,
            String nickname,
            String role
    ) {}
}
