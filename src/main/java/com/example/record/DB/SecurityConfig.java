package com.example.record.DB;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 401 일관 응답
    @Bean
    public AuthenticationEntryPoint authEntryPoint() {
        return (request, response, e) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 공개 엔드포인트
                        .requestMatchers(HttpMethod.POST, "/auth/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()

                        // ✅ OCR: 엔드포인트를 /ocr로 단순화 했으므로 /ocr(및 변형) 허용
                        .requestMatchers(HttpMethod.POST,
                                "/ocr", "/ocr/", "/ocr/**",
                                "/**/ocr", "/**/ocr/", "/**/ocr/**" // context-path(/api 등) 대응
                        ).permitAll()

                        // 🔒 STT: 운영 기본은 인증(테스트 시만 열고 싶으면 아래 3줄을 잠깐 주석 해제)
                        // .requestMatchers(HttpMethod.POST, "/stt").permitAll()
                        // .requestMatchers(HttpMethod.POST, "/stt/gpt").permitAll()
                        // .requestMatchers(HttpMethod.GET,  "/stt/list").permitAll()

                        .requestMatchers("/api/image/**").permitAll()

                        // 관리자 전용
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 나머지는 인증
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint()));

        // ✅ JWT 필터
        http.addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userRepository),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
