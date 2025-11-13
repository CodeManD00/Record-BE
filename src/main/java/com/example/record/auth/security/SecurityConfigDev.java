
package com.example.record.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * 개발 환경용 Spring Security 설정
 * 
 * 역할: 개발 환경에서 모든 요청을 허용하고, CORS를 활성화하여 프론트엔드와의 통신을 지원
 * 
 * 주요 설정:
 * - 모든 요청 허용 (permitAll)
 * - CORS 활성화 (React Native 앱에서 백엔드로 요청 가능하도록)
 * - CSRF 비활성화 (REST API이므로 불필요)
 */
@Configuration
@EnableWebSecurity
@Profile("dev")
@RequiredArgsConstructor
public class SecurityConfigDev {

    private final AuthenticationEntryPoint authEntryPoint;
    private final DevAuthBypassFilter devAuthBypassFilter;
    private final com.example.record.auth.jwt.JwtUtil jwtUtil;
    private final com.example.record.user.UserRepository userRepository;

    /**
     * CORS 설정
     * 
     * 역할: React Native 앱에서 백엔드 API로 요청을 보낼 수 있도록 CORS 헤더 설정
     * 
     * 설정 내용:
     * - 모든 Origin 허용 (개발 환경이므로)
     * - 모든 HTTP 메서드 허용 (GET, POST, PUT, DELETE 등)
     * - 모든 헤더 허용 (Authorization, Content-Type 등)
     * - Credentials 허용 (쿠키, 인증 정보 포함 가능)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 모든 Origin 허용 (개발 환경)
        // allowCredentials와 함께 사용할 수 없으므로 allowedOriginPatterns 사용
        configuration.setAllowedOriginPatterns(List.of("*"));
        // 모든 HTTP 메서드 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // 모든 헤더 허용
        configuration.setAllowedHeaders(List.of("*"));
        // 인증 정보 포함 허용 (JWT 토큰 등)
        configuration.setAllowCredentials(true);
        // Preflight 요청 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 경로에 CORS 설정 적용
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChainDev(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // dev: 전부 허용
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint));

        // dev: JWT 토큰이 없을 때만 가짜 인증 주입 (JWT 토큰이 있으면 실제 사용자 사용)
        // DevAuthBypassFilter를 먼저 추가하여 나중에 실행되도록 함
        http.addFilterBefore(devAuthBypassFilter, UsernamePasswordAuthenticationFilter.class);

        // JWT 인증 필터 추가 (JWT 토큰이 있으면 실제 사용자 인증)
        // JwtAuthenticationFilter를 나중에 추가하여 먼저 실행되도록 함 (JWT 토큰 우선 처리)
        http.addFilterBefore(
                new com.example.record.auth.jwt.JwtAuthenticationFilter(jwtUtil, userRepository),
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}
