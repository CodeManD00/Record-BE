package com.example.record.DB;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@Profile("prod")
@RequiredArgsConstructor
public class SecurityConfigProd {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final AuthenticationEntryPoint authEntryPoint;

    @Bean
    public SecurityFilterChain filterChainProd(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 공개 엔드포인트 (운영 최소 공개)
                        .requestMatchers(HttpMethod.POST, "/auth/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers("/api/image/**").permitAll()

                        // 운영에서는 보호
                        .requestMatchers("/ocr").authenticated()
                        .requestMatchers("/stt/**").authenticated()

                        // 관리자 전용
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 그 외 인증
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint));

        // JWT 필터
        http.addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userRepository),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
