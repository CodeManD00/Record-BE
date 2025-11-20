package com.example.record.auth.security;

import com.example.record.auth.jwt.JwtAuthenticationFilter;
import com.example.record.auth.jwt.JwtUtil;
import com.example.record.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final AuthenticationEntryPoint authEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())   // RN 테스트 중엔 열어도 됨
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // ========================
                        // 로그인/회원가입/이메일 인증
                        // ========================
                        .requestMatchers(
                                "/auth/login",
                                "/auth/signup",
                                "/auth/email/**",
                                "/auth/forgot-*",
                                "/auth/reset-password",
                                "/auth/password/**"
                        ).permitAll()

                        // ========================
                        // 개발 중 개방
                        // ========================
                        .requestMatchers("/ocr/**").permitAll()
                        .requestMatchers("/stt/**").permitAll()
                        .requestMatchers("/STTorText/**").permitAll()
                        .requestMatchers("/generate-image/**").permitAll()
                        .requestMatchers("/upload/**").permitAll()
                        .requestMatchers("/users/**").permitAll()
                        .requestMatchers("/test/**", "/api/test/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()

                        // ========================
                        // 나머지 모두 허용
                        // ========================
                        .anyRequest().permitAll()
                )

                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint));

        // JWT 필터 (현재 dev에서는 모든 요청에 대해 실행 → 문제 없음)
        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtUtil, userRepository),
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}
