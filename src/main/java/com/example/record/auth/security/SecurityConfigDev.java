package com.example.record.auth.security;

import com.example.record.auth.jwt.JwtAuthenticationFilter;
import com.example.record.auth.jwt.JwtUtil;
import com.example.record.user.UserRepository;
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

@Configuration
@EnableWebSecurity
@Profile("dev")
@RequiredArgsConstructor
public class SecurityConfigDev {

    private final AuthenticationEntryPoint authEntryPoint;
    private final DevAuthBypassFilter devAuthBypassFilter;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    /** CORS 설정 (개발 환경: 전부 허용) */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChainDev(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // dev: 전부 허용 (단, principal은 세팅됨)
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint));

        // 1) JWT 인증 필터: 토큰이 있으면 실제 사용자 인증
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtil, userRepository);
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // 2) DevAuthBypassFilter: JWT가 없고 인증이 비어 있을 때 dev 사용자 주입
        http.addFilterBefore(devAuthBypassFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
