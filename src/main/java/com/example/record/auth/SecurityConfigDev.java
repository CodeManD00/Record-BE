package com.example.record.auth;

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
@Profile("dev")
@RequiredArgsConstructor
public class SecurityConfigDev {

    private final AuthenticationEntryPoint authEntryPoint;
    private final DevAuthBypassFilter devAuthBypassFilter;

    @Bean
    public SecurityFilterChain filterChainDev(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // dev: 전부 허용
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint));

        // dev: 가짜 인증 주입(있어도 되고 없어도 됨)
        http.addFilterBefore(devAuthBypassFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
