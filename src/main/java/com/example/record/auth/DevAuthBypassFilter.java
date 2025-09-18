// src/main/java/com/example/record/auth/DevAuthBypassFilter.java
package com.example.record.auth;

import com.example.record.user.User;
import com.example.record.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Profile("dev")
@Component
@RequiredArgsConstructor
public class DevAuthBypassFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            // 1) dev 유저를 DB에서 찾고, 없으면 생성
            User devUser = userRepository.findByEmail("dev@local").orElseGet(() -> {
                User u = User.builder()
                        .email("dev@local")
                        .password(passwordEncoder.encode("devpass"))
                        .nickname("DEV")
                        .role("USER") // ADMIN 테스트면 "ADMIN"
                        .build();
                return userRepository.save(u);
            });

            // 2) 실제 DB 유저로 Authentication 구성
            var auth = new UsernamePasswordAuthenticationToken(
                    devUser,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + devUser.getRole()))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(req, res);
    }
}
