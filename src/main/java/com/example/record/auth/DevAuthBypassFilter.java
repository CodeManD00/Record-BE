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

        // Authorization 헤더가 있거나 이미 인증되어 있으면 우회 X
        boolean hasAuthHeader = req.getHeader("Authorization") != null;
        if (SecurityContextHolder.getContext().getAuthentication() == null && !hasAuthHeader) {

            // username 기반으로 dev 계정 조회/생성
            User devUser = userRepository.findByUsername("devuser").orElseGet(() -> {
                User u = User.builder()
                        .username("devuser")
                        .email("dev@local")
                        .password(passwordEncoder.encode("devpass"))
                        .nickname("DEV")
                        .build();
                return userRepository.save(u);
            });

            // role은 USER로 고정 (User 엔티티에 role 없음)
            var auth = new UsernamePasswordAuthenticationToken(
                    devUser,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(req, res);
    }
}
